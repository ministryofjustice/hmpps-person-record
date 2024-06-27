package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SEARCH_VERSION
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.COURT_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.LibraMessage
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

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
        "HIGH_CONFIDENCE_COUNT" to "0",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }
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
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }
    val matchingPerson = personEntities.filter { it.firstName.equals(firstName) }
    assertThat(matchingPerson.size).isEqualTo(1)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.99999999))
    stubMatchScore(matchResponse)

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
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
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
    publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.98883))
    stubMatchScore(matchResponse)

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
        "HIGH_CONFIDENCE_COUNT" to "0",
        "LOW_CONFIDENCE_COUNT" to "1",
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
  fun `should process and send duplicate telemetry`() {
    val firstName = randomFirstName()

    personRepository.saveAndFlush(
      PersonEntity.from(
        Person(
          firstName = firstName,
          lastName = "MORGAN",
          dateOfBirth = LocalDate.of(1975, 1, 1),
          addresses = listOf(Address("NT4 6YH")),
          sourceSystemType = LIBRA,
        ),
      ),
    )
    personRepository.saveAndFlush(
      PersonEntity.from(
        Person(
          firstName = firstName,
          lastName = "MORGAN",
          dateOfBirth = LocalDate.of(1975, 1, 1),
          addresses = listOf(Address("NT4 6YH")),
          sourceSystemType = LIBRA,
        ),
      ),
    )
    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.99999999,
        "1" to 0.99999999,
      ),
    )
    stubMatchScore(matchResponse)

    publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    checkTelemetry(
      CPR_MATCH_PERSON_DUPLICATE,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "PROBABILITY_SCORE" to "0.99999999",
      ),
      times = 2,
    )
  }

  @Test
  fun `should process multiple pages of candidates`() {
    val firstName = randomFirstName()
    repeat(100) {
      personRepository.saveAndFlush(
        PersonEntity.from(
          Person(
            firstName = firstName,
            lastName = "MORGAN",
            dateOfBirth = LocalDate.of(1975, 1, 1),
            addresses = listOf(Address("NT4 6YH")),
            sourceSystemType = LIBRA,
          ),
        ),
      )
    }

    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")
    val probabilities = mutableMapOf<String, Double>()
    repeat(50) { index ->
      probabilities[index.toString()] = 0.999999
    }
    val matchResponse = MatchResponse(matchProbabilities = probabilities)
    stubMatchScore(matchResponse)

    publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "100",
        "HIGH_CONFIDENCE_COUNT" to "100",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
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
    publishHMCTSMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    await.atMost(300, SECONDS) untilAsserted { assertThat(telemetryRepository.findAll().size).isEqualTo(3) }

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "RECORD_COUNT" to "1000000",
        "SEARCH_VERSION" to SEARCH_VERSION,
      ),
    )
  }

  private fun stubMatchScore(matchResponse: MatchResponse) {
    wiremock.stubFor(
      WireMock.post("/person/match")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(objectMapper.writeValueAsString(matchResponse)),
        ),
    )
  }
}
