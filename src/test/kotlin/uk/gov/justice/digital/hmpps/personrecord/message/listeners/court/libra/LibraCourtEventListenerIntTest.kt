package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.ORGANISATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomLibraSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle
import java.time.format.DateTimeFormatter

class LibraCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should create new person from Libra message`() {
    val title = randomTitle()
    val firstName = randomName()
    val forename2 = randomName()
    val forename3 = randomName()
    val lastName = randomName() + "'apostrophe"
    val postcode = randomPostcode()
    val pnc = randomLongPnc()
    val dateOfBirth = randomDate()
    val cId = randomCId()

    val buildingName = randomName()
    val buildingNumber = randomBuildingNumber()
    val thoroughfareName = randomName()
    val dependentLocality = randomName()
    val postTown = randomName()

    val sexCode = randomLibraSexCode()

    stubPersonMatchUpsert()
    stubPersonMatchScores()

    publishLibraMessage(
      libraHearing(
        title = title,
        firstName = firstName,
        foreName2 = forename2,
        foreName3 = forename3,
        lastName = lastName,
        cId = cId,
        dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        cro = "", pncNumber = pnc,
        postcode = postcode,
        defendantSex = sexCode.key,
        line1 = buildingName,
        line2 = buildingNumber,
        line3 = thoroughfareName,
        line4 = dependentLocality,
        line5 = postTown,
      ),
    )

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))
    checkEventLogExist(cId, CPRLogEvents.CPR_RECORD_CREATED)

    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))

    val person = awaitNotNull { personRepository.findByCId(cId) }

    val storedTitle = title.getTitle()
    assertThat(person.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
    assertThat(person.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
    assertThat(person.getPrimaryName().titleCode).isEqualTo(null)
    assertThat(person.getPrimaryName().firstName).isEqualTo(firstName)
    assertThat(person.getPrimaryName().middleNames).isEqualTo("$forename2 $forename3")
    assertThat(person.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(person.getPrimaryName().dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.getPrimaryName().sexCode).isEqualTo(sexCode.value)
    assertThat(person.getPnc()).isEqualTo(pnc)
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo(postcode)
    assertThat(person.addresses[0].buildingName).isEqualTo(buildingName)
    assertThat(person.addresses[0].buildingNumber).isEqualTo(buildingNumber)
    assertThat(person.addresses[0].thoroughfareName).isEqualTo(thoroughfareName)
    assertThat(person.addresses[0].dependentLocality).isEqualTo(dependentLocality)
    assertThat(person.addresses[0].postTown).isEqualTo(postTown)
    assertThat(person.addresses[0].subBuildingName).isNull()
    assertThat(person.addresses[0].county).isNull()
    assertThat(person.addresses[0].countryCode).isNull()
    assertThat(person.addresses[0].uprn).isNull()
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
    val updatedSexCode = randomLibraSexCode()
    val personEntity = createPersonWithNewKey(
      Person(
        firstName = firstName,
        lastName = lastName,
        addresses = listOf(Address(postcode = postcode)),
        dateOfBirth = dateOfBirth,
        sourceSystem = LIBRA,
        cId = cId,
        sexCode = randomLibraSexCode().value,
      ),
    )

    stubPersonMatchUpsert()
    stubNoMatchesPersonMatch(matchId = personEntity.matchId)

    val changedFirstName = randomName()
    val changedForename2 = ""
    val changedForename3 = randomName()
    val title = randomTitle()
    publishLibraMessage(libraHearing(defendantSex = updatedSexCode.key, firstName = changedFirstName, foreName2 = changedForename2, foreName3 = changedForename3, cId = cId, lastName = lastName, cro = "", pncNumber = "", postcode = postcode, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), title = title))

    checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "LIBRA", "C_ID" to cId))
    checkEventLogExist(cId, CPRLogEvents.CPR_RECORD_UPDATED)

    val person = awaitNotNull {
      personRepository.findByCId(cId)
    }
    val storedTitle = title.getTitle()
    assertThat(person.getPrimaryName().titleCodeLegacy?.code).isEqualTo(storedTitle.code)
    assertThat(person.getPrimaryName().titleCodeLegacy?.description).isEqualTo(storedTitle.description)
    assertThat(person.getPrimaryName().titleCode).isEqualTo(null)
    assertThat(person.getPrimaryName().firstName).isEqualTo(changedFirstName)
    assertThat(person.getPrimaryName().middleNames).isEqualTo(changedForename3)
    assertThat(person.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(person.getPrimaryName().dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(person.getPrimaryName().sexCode).isEqualTo(updatedSexCode.value)
    assertThat(person.addresses.size).isEqualTo(1)
    assertThat(person.addresses[0].postcode).isEqualTo(postcode)
    assertThat(person.sourceSystem).isEqualTo(LIBRA)
  }

  @Test
  fun `should process and create libra message and link to two different source system records in separate clusters and all three records end up on the same cluster`() {
    val cId = randomCId()

    val firstPersonFromProbation = createPersonWithNewKey(createRandomProbationPersonDetails())
    val secondPersonFromNomis = createPersonWithNewKey(createRandomPrisonPersonDetails())

    stubPersonMatchUpsert()
    stubXPersonMatches(aboveJoin = listOf(firstPersonFromProbation.matchId, secondPersonFromNomis.matchId))
    stubClusterIsValid()

    val personToCreateLibraHearing = libraHearing(firstName = randomName(), lastName = randomName(), cId = cId, dateOfBirth = randomDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))

    publishLibraMessage(personToCreateLibraHearing)

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "RECORD_COUNT" to "2",
        "ABOVE_JOIN_THRESHOLD_COUNT" to "2",
        "ABOVE_FRACTURE_THRESHOLD_COUNT" to "0",
        "BELOW_FRACTURE_THRESHOLD_COUNT" to "0",
        "C_ID" to cId,
      ),
      2,
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      mapOf(
        "SOURCE_SYSTEM" to LIBRA.name,
        "CLUSTER_SIZE" to "1",
        "UUID" to firstPersonFromProbation.personKey?.personUUID.toString(),
      ),
    )

    firstPersonFromProbation.personKey?.assertClusterIsOfSize(3)
  }

  @Test
  fun `should republish organisation defendant from libra without creating a person record`() {
    val cId = randomCId()
    publishLibraMessage(libraHearing(cId = cId, defendantType = ORGANISATION))

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())
    assertThat(personRepository.findByCId(cId)).isNull()

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }

    val libraMessage: String = sqsMessage?.message!!

    assertThat(libraMessage.contains(cId)).isEqualTo(true)
    assertThat(libraMessage.contains("cprUUID")).isEqualTo(false)
  }

  @Test
  fun `should republish defendant with no firstname, middlename and date of birth from libra without creating a person record`() {
    val cId = randomCId()
    val lastName = randomName()
    publishLibraMessage(libraHearing(cId = cId, lastName = lastName, defendantType = PERSON))

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val courtMessage = testOnlyCourtEventsQueue?.sqsClient?.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testOnlyCourtEventsQueue?.queueUrl).build())
    assertThat(personRepository.findByCId(cId)).isNull()

    val sqsMessage = courtMessage?.get()?.messages()?.first()?.let { objectMapper.readValue<SQSMessage>(it.body()) }

    val libraMessage: String = sqsMessage?.message!!

    assertThat(libraMessage.contains(cId)).isEqualTo(true)
    assertThat(libraMessage.contains(lastName)).isEqualTo(true)
    assertThat(libraMessage.contains("cprUUID")).isEqualTo(false)
    assertThat(personRepository.findByCId(cId)).isNull()
  }

  @Test
  fun `should publish incoming person defendant to court topic with UUID`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val firstName = randomName()
    val lastName = randomName() + "'apostrophe"
    val postcode = randomPostcode()
    val pnc = randomLongPnc()
    val dateOfBirth = randomDate()
    val cId = randomCId()

    publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode))

    expectOneMessageOn(testOnlyCourtEventsQueue)

    val cprUUID = awaitNotNull { personRepository.findByCId(cId) }.personKey?.personUUID.toString()
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
      val pnc = randomLongPnc()
      val dateOfBirth = randomDate()
      val cId = randomCId()

      publishLibraMessage(libraHearing(firstName = firstName, lastName = lastName, cId = cId, dateOfBirth = dateOfBirth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), cro = "", pncNumber = pnc, postcode = postcode))

      checkEventLog(cId, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
        assertThat(eventLogs.size).isEqualTo(1)
        val createdLog = eventLogs.first()
        assertThat(createdLog.pncs).isEqualTo(arrayOf(pnc))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.dateOfBirth).isEqualTo(dateOfBirth)
        assertThat(createdLog.sourceSystem).isEqualTo(LIBRA)
        assertThat(createdLog.postcodes).isEqualTo(arrayOf(postcode))
        assertThat(createdLog.personUUID).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }
      checkEventLogExist(cId, CPRLogEvents.CPR_UUID_CREATED)
    }
  }
}
