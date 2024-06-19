package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
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
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchRequestData
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponseData
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SEARCH_VERSION
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE_SUMMARY
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.COURT_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.LibraMessage
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

    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")
    val messageId = publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "0",
        "MESSAGE_ID" to messageId,
      )
    )
    checkTelemetry(
      CPR_MATCH_SCORE_SUMMARY,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "HIGH_CONFIDENCE_TOTAL" to "0",
        "LOW_CONFIDENCE_TOTAL" to "0",
      )
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
    assertThat(person.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo("NT4 6YH")
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should process and update libra messages`() {
    val firstName = randomFirstName()

    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")
    val messageId1 = publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId1,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    val matchingPerson = personEntities.filter { it.firstName.equals(firstName) }
    assertThat(matchingPerson.size).isEqualTo(1)


    val stubRequest = LibraMessage(firstName = firstName, cro = "", pncNumber = "", dateOfBirth = "1975-01-01")
    val matchRequest = createMatchRequest(stubRequest, stubRequest)
    val matchResponse = MatchResponse(MatchResponseData(value = "0.99999999"))
    stubMatchScore(matchRequest, matchResponse)

    val messageId2 = publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId2,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "1",
        "MESSAGE_ID" to messageId2,
      )
    )
    checkTelemetry(
      CPR_MATCH_SCORE_SUMMARY,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "HIGH_CONFIDENCE_TOTAL" to "1",
        "LOW_CONFIDENCE_TOTAL" to "0",
      )
    )
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    val updatedPersonEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }

    val updatedMatchingPerson = updatedPersonEntities.filter { it.firstName.equals(firstName) }
    assertThat(updatedMatchingPerson.size).isEqualTo(1)
    val person = updatedMatchingPerson[0]
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo("MORGAN")
    assertThat(person.dateOfBirth).isEqualTo(LocalDate.of(1975, 1, 1))
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo("NT4 6YH")
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should process and create new person with low score`() {
    val firstName = randomFirstName()

    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")
    val messageId1 = publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId1,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    val stubRequest = LibraMessage(firstName = firstName, cro = "", pncNumber = "", dateOfBirth = "1975-01-01")
    val matchRequest = createMatchRequest(stubRequest, stubRequest)
    val matchResponse = MatchResponse(MatchResponseData(value = "0.98883"))
    stubMatchScore(matchRequest, matchResponse)

    val messageId2 = publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId2,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"), times = 2)

    val updatedPersonEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }

    val updatedMatchingPerson = updatedPersonEntities.filter { it.firstName.equals(firstName) }
    assertThat(updatedMatchingPerson.size).isEqualTo(2)
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

    val libraMessage = LibraMessage(firstName = "Jayne", lastName = "Smith", postcode = "LS2 1AB")
    val messageId = publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

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
    val firstMessageId = publishHMCTSMessage(libraHearing(LibraMessage(firstName = "Steve", lastName = "Micheal", postcode = "RF14 5DG")), LIBRA_COURT_CASE)
    val secondMessageId = publishHMCTSMessage(libraHearing(LibraMessage(firstName = "Geoff", lastName = "Smith", postcode = "LS1 1AB", pncNumber = matchingPnc, cro = matchingCro)), LIBRA_COURT_CASE)
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
        LibraMessage(
          firstName = randomFirstName(),
          lastName = "Smythe",
          postcode = "LS1 1AB",
          pncNumber = matchingPnc,
          cro = matchingCro,
        )
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
  fun `should process concurrent unique libra messages with data reads`() {
    val blitzer = Blitzer(50, 5)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(
          buildPublishRequest(LibraMessage(firstName = randomFirstName(), cro = "", pncNumber = ""))
        )?.get()
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

  private fun buildPublishRequest(libraMessage: LibraMessage = LibraMessage()): PublishRequest? = PublishRequest.builder()
    .topicArn(courtCaseEventsTopic?.arn)
    .message(libraHearing(libraMessage))
    .messageAttributes(
      mapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(LIBRA_COURT_CASE.name).build(),
      ),
    )
    .build()

  private fun createMatchRequest(person: LibraMessage, matchingPerson: LibraMessage): MatchRequest {
    return MatchRequest(
      firstName = MatchRequestData(person.firstName, matchingPerson.firstName),
      surname = MatchRequestData(person.lastName, matchingPerson.lastName),
      dateOfBirth = MatchRequestData(person.dateOfBirth, matchingPerson.dateOfBirth),
      pncNumber = MatchRequestData(person.pncNumber, matchingPerson.pncNumber),
      uniqueId = MatchRequestData("defendant1", "defendant2"),
    )
  }

  private fun stubMatchScore(matchRequest: MatchRequest, matchResponse: MatchResponse) {
    wiremock.stubFor(
      WireMock.post("/match")
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(matchRequest)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(matchResponse)),
        ),
    )
  }
}
