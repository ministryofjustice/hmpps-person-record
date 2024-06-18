package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SEARCH_VERSION
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.COURT_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.text.Charsets.UTF_8

class LibraCourtCaseListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  override fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should process libra messages`() {
    val firstName = randomFirstName()
    val messageId = publishHMCTSMessage(libraHearing(firstName = firstName), LIBRA_COURT_CASE)

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    val matchingPerson = personEntities.filter { it.firstName.equals(firstName) }
    assertThat(matchingPerson.size).isEqualTo(1)

    val person = matchingPerson[0]
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo("MORGAN")
    assertThat(person.pnc).isEqualTo(PNCIdentifier.from("2003/0011985X"))
    assertThat(person.cro).isEqualTo(CROIdentifier.from("85227/65L"))
    assertThat(person.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo("NT4 6YH")
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  @Disabled("Disabling as it takes too long to run in a CI context - out of memory errors")
  fun `should process libra with large amount of candidates - CPR-354`() {
    await untilAsserted { assertThat(personRepository.findAll().size).isEqualTo(0) }
    val blitzer = Blitzer(1000000, 10)
    try {
      blitzer.blitz {
        personRepository.saveAndFlush(
          PersonEntity.from(
            Person(
              firstName = "Jane",
              lastName = "Smith",
              addresses = listOf(Address(postcode = "LS1 1AB")),
              sourceSystemType = SourceSystemType.HMCTS,
            ),
          ),
        )
      }
    } finally {
      blitzer.shutdown()
    }

    await.atMost(300, SECONDS) untilAsserted { assertThat(personRepository.findAll().size).isEqualTo(1000000) }

    val messageId = publishHMCTSMessage(libraHearing(firstName = "Jayne", lastName = "Smith", postcode = "LS2 1AB"), LIBRA_COURT_CASE)

    await.atMost(300, SECONDS) untilAsserted { assertThat(telemetryRepository.findAll().size).isEqualTo(3) }

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "RECORD_COUNT" to "1000000",
        "SEARCH_VERSION" to SEARCH_VERSION,
        "MESSAGE_ID" to messageId,
      ),
    )
  }

  @Test
  fun `should send correct telemetry for candidate search when one match found`() {
    val allPNCs = Files.readAllLines(Paths.get("src/test/resources/valid_pncs.csv"), UTF_8)
    val matchingPnc = allPNCs.get((0..allPNCs.size).random())
    val allCROs = Files.readAllLines(Paths.get("src/test/resources/valid_cros.csv"), UTF_8)
    val matchingCro = allCROs.get((0..allCROs.size).random())
    val firstMessageId = publishHMCTSMessage(libraHearing(firstName = "Steve", lastName = "Micheal", postcode = "RF14 5DG"), LIBRA_COURT_CASE)
    val secondMessageId = publishHMCTSMessage(libraHearing(firstName = "Geoff", lastName = "Smith", postcode = "LS1 1AB", pncNumber = matchingPnc, cro = matchingCro), LIBRA_COURT_CASE)
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "SEARCH_VERSION" to SEARCH_VERSION,
        "MESSAGE_ID" to firstMessageId,
      ),
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "SEARCH_VERSION" to SEARCH_VERSION,
        "MESSAGE_ID" to secondMessageId,
      ),
    )
    val thirdMessageId = publishHMCTSMessage(
      libraHearing(
        firstName = randomFirstName(),
        lastName = "Smythe",
        postcode = "LS1 1AB",
        pncNumber = matchingPnc,
        cro = matchingCro,
      ),
      LIBRA_COURT_CASE,
    )

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "RECORD_COUNT" to "1",
        "SEARCH_VERSION" to SEARCH_VERSION,
        "MESSAGE_ID" to thirdMessageId,
      ),
    )
  }

  @Test
  fun `should process concurrent libra messages with data reads`() {
    val blitzer = Blitzer(50, 5)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(buildPublishRequest())?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    await untilCallTo {
      courtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(courtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilCallTo {
      courtCaseEventsQueue?.sqsDlqClient?.countMessagesOnQueue(courtCaseEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "LIBRA"),
      times = 50,
    )
  }

  private fun buildPublishRequest(): PublishRequest? = PublishRequest.builder()
    .topicArn(courtCaseEventsTopic?.arn)
    .message(libraHearing())
    .messageAttributes(
      mapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(LIBRA_COURT_CASE.name).build(),
      ),
    )
    .build()
}
