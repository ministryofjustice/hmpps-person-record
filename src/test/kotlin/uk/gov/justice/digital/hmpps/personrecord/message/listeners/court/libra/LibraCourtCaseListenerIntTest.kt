package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

class LibraCourtCaseListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    personRepository.deleteAll()
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should process libra messages`() {
    val messageId = publishHMCTSMessage(libraHearing(), LIBRA_COURT_CASE)

    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
      ),
    )

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }

    assertThat(personEntities.size).isEqualTo(1)

    val person = personEntities[0]
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo("MORGAN")
    assertThat(person.pnc).isEqualTo(PNCIdentifier.from("2003/0011985X"))
    assertThat(person.cro).isEqualTo(CROIdentifier.from("85227/65L"))
    assertThat(person.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo("NT4 6YH")
  }

  @Test
  fun `should process send correct telemetry for candidate search`() {
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
    personRepository.saveAndFlush(
      PersonEntity.from(
        Person(
          firstName = "Steve",
          lastName = "Micheal",
          addresses = listOf(Address(postcode = "RF14 5DG")),
          sourceSystemType = SourceSystemType.HMCTS,
        ),
      ),
    )

    await untilAsserted { assertThat(personRepository.findAll().size).isEqualTo(2) }

    publishHMCTSMessage(
      libraHearing(
        firstName = "Jane",
        lastName = "Smythe",
        postcode = "LS1 1AB",
      ),
      LIBRA_COURT_CASE,
    )

    checkTelemetry(
      TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "HMCTS",
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "RECORD_COUNT" to "1",
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
      mapOf("SOURCE_SYSTEM" to "HMCTS"),
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
