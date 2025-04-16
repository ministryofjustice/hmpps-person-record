package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.ORGANISATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
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
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    val messageId = publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode))

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId,
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))
    checkEventLogExist(cId, CPRLogEvents.CPR_RECORD_CREATED)

    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))

    val person = awaitNotNullPerson { personRepository.findByCId(cId) }

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
        cId = cId,
      ),
    )

    stubPersonMatchUpsert()
    stubNoMatchesPersonMatch(matchId = personEntity.matchId)

    val updatedMessage = publishLibraMessage(libraHearing(firstName = firstName, cId = cId, lastName = lastName, cro = "", pncNumber = "", postcode = postcode, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId,
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to updatedMessage,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))
    checkEventLogExist(cId, CPRLogEvents.CPR_RECORD_UPDATED)

    val person = awaitNotNullPerson {
      personRepository.findByCId(cId)
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
    val cId = randomCId()
    val personFromProbation = Person(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      addresses = listOf(Address(postcode = randomPostcode())),
      sourceSystem = DELIUS,
    )
    val personKeyEntity = createPersonKey()
    val existingPerson = createPerson(personFromProbation, personKeyEntity = personKeyEntity)

    stubPersonMatchUpsert()
    stubOnePersonMatchHighConfidenceMatch(matchedRecord = existingPerson.matchId)

    val messageId = publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = ""))
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId,
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))
    checkEventLogExist(cId, CPRLogEvents.CPR_RECORD_CREATED)
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "0",
        "C_ID" to cId,
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

    val personKey = personKeyRepository.findByPersonId(personKeyEntity.personId)
    assertThat(personKey?.personEntities?.size).isEqualTo(2)
  }

  @Test
  fun `should republish organisation defendant from libra without creating a person record`() {
    val cId = randomCId()
    val messageId = publishLibraMessage(libraHearing(cId = cId, defendantType = ORGANISATION))

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())
    assertThat(personRepository.findByCId(cId)).isNull()

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }

    val libraMessage: String = sqsMessage?.message!!

    assertThat(libraMessage.contains(cId)).isEqualTo(true)
    assertThat(libraMessage.contains("cprUUID")).isEqualTo(false)
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "C_ID" to cId,
        "EVENT_TYPE" to LIBRA_COURT_CASE.name,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to LIBRA.name,
      ),
      times = 0,
    )
  }

  @Test
  fun `should publish incoming person defendant to court topic with UUID`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val firstName = randomName()
    val lastName = randomName() + "'apostrophe"
    val postcode = randomPostcode()
    val pnc = randomPnc()
    val dateOfBirth = randomDate()
    val cId = randomCId()

    publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode))

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val cprUUID = awaitNotNullPerson { personRepository.findByCId(cId) }.personKey?.personId.toString()
    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }

    val libraMessage: String = sqsMessage?.message!!

    assertThat(libraMessage.contains(cId)).isEqualTo(true)
    assertThat(libraMessage.contains(cprUUID)).isEqualTo(true)

    assertThat(sqsMessage.getHearingEventType()).isNull()
    assertThat(sqsMessage.getEventType()).isEqualTo("libra.case.received")
    assertThat(sqsMessage.getMessageType()).isEqualTo(LIBRA_COURT_CASE.name)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId),
    )
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should save record details in event log on create`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val firstName = randomName()
      val lastName = randomName()
      val postcode = randomPostcode()
      val pnc = randomPnc()
      val dateOfBirth = randomDate()
      val cId = randomCId()

      publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode))

      checkEventLog(cId, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
        assertThat(eventLogs?.size).isEqualTo(1)
        val createdLog = eventLogs!!.first()
        assertThat(createdLog.pncs).isEqualTo(arrayOf(pnc))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.dateOfBirth).isEqualTo(dateOfBirth)
        assertThat(createdLog.sourceSystem).isEqualTo(LIBRA)
        assertThat(createdLog.postcodes).isEqualTo(arrayOf(postcode))
        assertThat(createdLog.uuid).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }
      checkEventLogExist(cId, CPRLogEvents.CPR_UUID_CREATED)
    }
  }
}
