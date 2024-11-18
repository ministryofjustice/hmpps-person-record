package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQueries
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQueryType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_LOW_SELF_SCORE_NOT_CREATING_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_SELF_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.LibraMessage
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
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
    val lastName = randomName() + "'apostrophe"
    val postcode = randomPostcode()
    val pnc = randomPnc()
    val dateOfBirth = randomDate()
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
        "UUID_COUNT" to "0",
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
    assertThat(person.defendantId).isNotNull()
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo(postcode)
    assertThat(person.personKey).isNotNull()
    assertThat(person.selfMatchScore).isEqualTo(0.9999)
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should process and update libra messages`() {
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    val dateOfBirth = randomDate()
    val libraDefendantId = randomDefendantId()
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        defendantId = libraDefendantId,
        addresses = listOf(Address(postcode = postcode)),
        dateOfBirth = dateOfBirth,
        sourceSystemType = LIBRA,
      ),
      personKeyEntity = createPersonKey(),
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
        "UUID_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "0",
        "QUERY" to PersonQueryType.FIND_CANDIDATES_BY_SOURCE_SYSTEM.name,
      ),
    )
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    await untilCallTo {
      reclusterEventsQueue?.sqsClient?.countMessagesOnQueue(reclusterEventsQueue!!.queueUrl)?.get()
    } matches { it == 1 }

    val person = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(libraDefendantId)
    }

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
    val dateOfBirth = randomDate()
    val personFromProbation = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      addresses = listOf(Address(postcode = "NT4 6YH")),
      sourceSystemType = DELIUS,
    )
    val personKeyEntity = createPersonKey()
    createPerson(personFromProbation, personKeyEntity = personKeyEntity)

    val highConfidenceMatchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.9999999))
    stubMatchScore(highConfidenceMatchResponse)

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
        "CLUSTER_SIZE" to "1",
        "UUID" to personKeyEntity.personId.toString(),
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

    val personKey = personKeyRepository.findByPersonId(personKeyEntity.personId)
    assertThat(personKey?.personEntities?.size).isEqualTo(2)
  }

  @Test
  fun `should process and create new person when has a match with low score`() {
    stubSelfMatchScore(nextScenarioState = "checkRecordExistCandidateCheck")
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        addresses = listOf(Address(postcode = postcode)),
        sourceSystemType = LIBRA,
      ),
      personKeyEntity = createPersonKey(),
    )
    val matchResponse = MatchResponse(matchProbabilities = mutableMapOf("0" to 0.98883))
    stubMatchScore(matchResponse, currentScenarioState = "checkRecordExistCandidateCheck", nextScenarioState = "candidateUuidCheck")
    stubMatchScore(matchResponse, currentScenarioState = "candidateUuidCheck")

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
      CPR_SELF_MATCH,
      mapOf(
        "IS_ABOVE_SELF_MATCH_THRESHOLD" to "true",
        "PROBABILITY_SCORE" to "0.9999",
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
    checkTelemetry(
      CPR_MATCH_SCORE,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "PROBABILITY_SCORE" to "0.98883",
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
          addresses = listOf(Address(postcode = "NT4 6YH")),
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
          addresses = listOf(Address(postcode = "NT4 6YH")),
          sourceSystemType = LIBRA,
        ),
      ),
    )
    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")

    val twoHighConfidenceMatchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.99999999,
        "1" to 0.99999999,
      ),
    )
    stubMatchScore(twoHighConfidenceMatchResponse)

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
  fun `should process large number of candidates`() {
    val firstName = randomName()
    repeat(300) {
      personRepository.saveAndFlush(
        PersonEntity.from(
          Person(
            firstName = firstName,
            lastName = "MORGAN",
            dateOfBirth = LocalDate.of(1975, 1, 1),
            addresses = listOf(Address(postcode = "NT4 6YH")),
            sourceSystemType = LIBRA,
          ),
        ),
      )
    }

    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")
    val probabilities = mutableMapOf<String, Double>()
    repeat(100) { index ->
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
        "RECORD_COUNT" to "300",
        "HIGH_CONFIDENCE_COUNT" to "300",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
  }

  @Test
  @Disabled("Disabling as it takes too long to run in a CI context - out of memory errors")
  fun `should process libra with large amount of candidates - CPR-354`() {
    personRepository.deleteAllInBatch()
    personKeyRepository.deleteAllInBatch()
    await untilAsserted { assertThat(personRepository.count()).isEqualTo(0) }

    // Create 200,000 random records
    blitz(200000, 10) {
      createPerson(
        Person(
          firstName = randomName(),
          lastName = randomName(),
          addresses = listOf(Address(postcode = randomPostcode())),
          references = listOf(
            Reference(IdentifierType.PNC, randomPnc()),
            Reference(IdentifierType.PNC, randomPnc()),
            Reference(IdentifierType.CRO, randomCro()),
            Reference(IdentifierType.CRO, randomCro()),
          ),
          dateOfBirth = randomDate(),
          sourceSystemType = LIBRA,
        ),
        personKeyEntity = createPersonKey(),
      )
    }

    // Create 200 random records to match against
    blitz(200, 10) {
      createPerson(
        Person(
          firstName = "Jane",
          lastName = "Smith",
          dateOfBirth = LocalDate.of(1975, 1, 1),
          addresses = listOf(Address(postcode = "LS1 1AB")),
          sourceSystemType = LIBRA,
        ),
      )
    }

    val probabilities = mutableMapOf<String, Double>()
    repeat(100) { index ->
      probabilities[index.toString()] = 0.999999
    }
    val matchResponse = MatchResponse(matchProbabilities = probabilities)
    stubMatchScore(matchResponse)

    await.atMost(300, SECONDS) untilAsserted { assertThat(personRepository.count()).isEqualTo(200200) }

    val libraMessage = LibraMessage(firstName = "Jayne", lastName = "Smith", postcode = "LS2 1AB")
    publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)

    await.atMost(300, SECONDS) untilAsserted { assertThat(telemetryRepository.count()).isEqualTo(3) }

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "RECORD_COUNT" to "200",
        "SEARCH_VERSION" to PersonQueries.SEARCH_VERSION,
        "QUERY" to "FIND_CANDIDATES_BY_SOURCE_SYSTEM",
      ),
    )
  }

  @Test
  fun `should not conduct a candidate search when self match does not meet threshold and then create a record`() {
    stubSelfMatchScore(0.5123)
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    val pnc = randomPnc()
    val dateOfBirth = randomDate()
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
      CPR_SELF_MATCH,
      mapOf(
        "IS_ABOVE_SELF_MATCH_THRESHOLD" to "false",
        "PROBABILITY_SCORE" to "0.5123",
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
      times = 0,
    )
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    checkTelemetry(
      CPR_LOW_SELF_SCORE_NOT_CREATING_UUID,
      mapOf(
        "PROBABILITY_SCORE" to "0.5123",
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA"), times = 0)

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAll()
    }
    val matchingPerson = personEntities.filter { it.firstName.equals(firstName) }
    assertThat(matchingPerson.size).isEqualTo(1)

    val person = matchingPerson[0]
    assertThat(person.defendantId).isNotNull()
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo(postcode)
    assertThat(person.personKey).isNull()
    assertThat(person.selfMatchScore).isEqualTo(0.5123)
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }
}
