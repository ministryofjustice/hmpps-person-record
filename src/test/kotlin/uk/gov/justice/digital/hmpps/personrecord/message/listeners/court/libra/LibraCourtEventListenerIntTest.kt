package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.format.DateTimeFormatter

class LibraCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should create new person from Libra message`() {
    val firstName = randomName()
    val lastName = randomName() + "'apostrophe"
    val postcode = randomPostcode()
    val pnc = randomPnc()
    val dateOfBirth = randomDate()
    val cId = randomCId()
    val messageId = publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode))

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId.toString(),
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId.toString()))
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId.toString()))

    val person = awaitNotNullPerson { personRepository.findByCId(cId.toString()) }

    assertThat(person.title).isEqualTo("Mr")
    assertThat(person.lastName).isEqualTo(lastName)
    assertThat(person.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.references.getType(PNC).first().identifierValue).isEqualTo(pnc)
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
    val cId = randomCId()
    val personEntity = createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        addresses = listOf(Address(postcode = postcode)),
        dateOfBirth = dateOfBirth,
        sourceSystem = LIBRA,
        cId = cId.toString(),
      ),
    )

    stubOneHighConfidenceMatch()

    val updatedMessage = publishLibraMessage(libraHearing(firstName = firstName, cId = cId, lastName = lastName, cro = "", pncNumber = "", postcode = postcode, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId.toString(),
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to updatedMessage,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId.toString()))

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personEntity.personKey?.personId.toString()),
    )

    val person = awaitNotNullPerson {
      personRepository.findByCId(cId.toString())
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
    telemetryRepository.deleteAll()
    val firstName = randomName()
    val lastName = randomName()
    val dateOfBirth = randomDate()
    val cId = randomCId()
    val personFromProbation = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      addresses = listOf(Address(postcode = randomPostcode())),
      sourceSystem = DELIUS,
    )
    val personKeyEntity = createPersonKey()
    createPerson(personFromProbation, personKeyEntity = personKeyEntity)

    stubOneHighConfidenceMatch()

    val messageId = publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = ""))
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId.toString(),
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId.toString()))
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
}
