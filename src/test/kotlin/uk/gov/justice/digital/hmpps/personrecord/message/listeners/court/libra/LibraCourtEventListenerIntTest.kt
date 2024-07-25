package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SEARCH_VERSION
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries.PersonQueryType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.LibraMessage
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateOfBirth
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.SECONDS

class LibraCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should process libra messages`() {
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    val pnc = randomPnc()
    val dateOfBirth = randomDateOfBirth()
    val libraMessage = LibraMessage(firstName = firstName, lastName = lastName, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode)
    val messageId = publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    checkTelemetry(
      MESSAGE_RECEIVED,
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
      times = 2,
    )
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }
    val matchingPerson = personEntities.filter { it.firstName.equals(firstName) }
    assertThat(matchingPerson.size).isEqualTo(1)

    val person = matchingPerson[0]
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.getReferencesOfType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo(postcode)
    assertThat(person.personKey).isNotNull()
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should process and update libra messages`() {
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    val dateOfBirth = randomDateOfBirth()
    createAndSavePersonWithUuid(
      Person(
        firstName = firstName,
        lastName = lastName,
        addresses = listOf(Address(postcode)),
        dateOfBirth = dateOfBirth,
        sourceSystemType = LIBRA,
      ),
    )
    val libraMessage = LibraMessage(firstName = firstName, lastName = lastName, cro = "", pncNumber = "", postcode = postcode, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.99999999))
    stubMatchScore(matchResponse)

    val messageId2 = publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId2,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )
    checkTelemetry(
      CPR_MATCH_SCORE,
      mapOf(
        "PROBABILITY_SCORE" to "0.99999999",
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
        "QUERY" to PersonQueryType.FIND_CANDIDATES_BY_SOURCE_SYSTEM.name
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
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo(postcode)
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should process and create libra message and link to different source system record`() {
    val firstName = randomName()
    val lastName = randomName()
    val dateOfBirth = randomDateOfBirth()
    val person = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      addresses = listOf(Address("NT4 6YH")),
      sourceSystemType = DELIUS,
    )
    val uuid = createAndSavePersonWithUuid(person)

    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(matchResponse)

    val libraMessage = LibraMessage(firstName = firstName, lastName = lastName, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = "")
    val messageId1 = publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId1,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "UUID" to uuid.toString(),
      ),
    )
    checkTelemetry(
      CPR_MATCH_PERSON_DUPLICATE,
      mapOf(
        "SOURCE_SYSTEM" to "DELIUS",
        "PROBABILITY_SCORE" to "0.9999999",
      ),
      times = 0,
    )

    val personKey = personKeyRepository.findByPersonId(uuid)
    assertThat(personKey.personEntities.size).isEqualTo(2)
  }

  @Test
  fun `should process and create new person with low score`() {
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    createAndSavePersonWithUuid(
      Person(
        firstName = firstName,
        lastName = lastName,
        addresses = listOf(Address(postcode)),
        sourceSystemType = LIBRA,
      ),
    )
    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.98883))
    stubMatchScore(matchResponse)

    val libraMessage = LibraMessage(firstName = firstName, lastName = lastName, postcode = postcode, cro = "", pncNumber = "")
    val messageId2 = publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(
      MESSAGE_RECEIVED,
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
      times = 2,
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
  }

  @Test
  fun `should process and send duplicate telemetry`() {
    val firstName = randomName()

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

    publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
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
    val firstName = randomName()
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

    publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
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
              sourceSystemType = COMMON_PLATFORM,
            ),
          ),
        )
      }
    } finally {
      blitzer.shutdown()
    }

    await.atMost(300, SECONDS) untilAsserted { assertThat(personRepository.findAll().size).isEqualTo(1000000) }

    val libraMessage = LibraMessage(firstName = "Jayne", lastName = "Smith", postcode = "LS2 1AB")
    publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

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
}
