package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQueryType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_SCORE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.LibraMessage
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.jvm.optionals.getOrNull

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

    val person = awaitNotNullPerson { personRepository.findOne(Example.of(PersonEntity(firstName = firstName, sourceSystem = LIBRA, version = 1))).getOrNull() }

    assertThat(person.defendantId).isNotNull()
    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
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
    val dateOfBirth = randomDate()
    val libraDefendantId = randomDefendantId()
    val personEntity = createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        defendantId = libraDefendantId,
        addresses = listOf(Address(postcode = postcode)),
        dateOfBirth = dateOfBirth,
        sourceSystem = LIBRA,
      ),
      personKeyEntity = createPersonKey(),
    )
    val libraMessage = LibraMessage(firstName = firstName, lastName = lastName, cro = "", pncNumber = "", postcode = postcode, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

    stubOneHighConfidenceMatch()

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
        "PROBABILITY_SCORE" to "0.999999",
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

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personEntity.personKey?.personId.toString()),
    )

    val person = awaitNotNullPerson {
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
      sourceSystem = DELIUS,
    )
    val personKeyEntity = createPersonKey()
    createPerson(personFromProbation, personKeyEntity = personKeyEntity)

    stubOneHighConfidenceMatch()

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
  fun `should process and create new person with uuid when has a match with low score`() {
    val firstName = randomName()
    val lastName = randomName()
    val postcode = randomPostcode()
    createPerson(
      Person(
        firstName = firstName,
        lastName = lastName,
        addresses = listOf(Address(postcode = postcode)),
        sourceSystem = LIBRA,
      ),
      personKeyEntity = createPersonKey(),
    )
    stubOneLowConfidenceMatch()

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
    checkTelemetry(
      CPR_MATCH_SCORE,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "PROBABILITY_SCORE" to "0.988899",
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
          sourceSystem = LIBRA,
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
          sourceSystem = LIBRA,
        ),
      ),
    )
    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")

    stubXHighConfidenceMatches(2)

    publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))

    checkTelemetry(
      CPR_MATCH_PERSON_DUPLICATE,
      mapOf(
        "SOURCE_SYSTEM" to "LIBRA",
        "PROBABILITY_SCORE" to "0.999999",
      ),
      times = 2,
    )
  }

  @Test
  fun `should process large number of candidates`() {
    val firstName = randomName()
    repeat(110) {
      personRepository.saveAndFlush(
        PersonEntity.from(
          Person(
            firstName = firstName,
            lastName = "MORGAN",
            dateOfBirth = LocalDate.of(1975, 1, 1),
            addresses = listOf(Address(postcode = "NT4 6YH")),
            sourceSystem = LIBRA,
          ),
        ),
      )
    }

    val libraMessage = LibraMessage(firstName = firstName, cro = "", pncNumber = "")

    stubXHighConfidenceMatches(100)

    publishCourtMessage(libraHearing(libraMessage), LIBRA_COURT_CASE)
    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA"))
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "110",
        "HIGH_CONFIDENCE_COUNT" to "110",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
  }
}
