package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.QUEUE_ADMIN
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@AutoConfigureWebTestClient
class PersonDomainEventPublisherIntTest : MessagingTestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  internal lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  fun WebTestClient.RequestHeadersSpec<*>.authorised(roles: List<String> = listOf(QUEUE_ADMIN)): WebTestClient.RequestBodySpec = headers(jwtAuthorisationHelper.setAuthorisationHeader(roles = roles)) as WebTestClient.RequestBodySpec

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Nested
  inner class PersonCreatedScenarios {

    @Test
    fun `should publish a CPR person created domain event when a person is created in nomis`() {
      val prisonNumber = randomPrisonNumber()

      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
      publishDomainEvent(
        PrisonPersonCreated(
          personReference = PersonReference(
            listOf(
              PersonIdentifier(
                "NOMS",
                prisonNumber,
              ),
            ),
          ),
        ),
      )

      awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
      )
      val sqsMessage =
        rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_PERSON_CREATED))
      val domainEvent: CprPersonCreated = jsonMapper.readValue<CprPersonCreated>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_PERSON_CREATED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber")
      assertThat(domainEvent.description).isEqualTo("A prison person record has been created")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
    }

    @Test
    fun `should publish a CPR person created domain event when a person is created in delius`() {
      val crn = randomCrn()

      probationCreateEventAndResponseSetup(ApiResponseSetup.from(createRandomProbationCase(crn)))

      awaitNotNull { personRepository.findByCrn(crn) }

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
      )
      val sqsMessage =
        rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_CREATED))
      val domainEvent: CprPersonCreated = jsonMapper.readValue<CprPersonCreated>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_CREATED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn")
      assertThat(domainEvent.description).isEqualTo("A probation person record has been created")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("CRN")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(crn)
    }

    @Test
    fun `should publish a CPR person created domain event when a person is created in common platform`() {
      val defendantId = randomDefendantId()

      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              defendantId = defendantId,
              cro = randomCro(),
              pnc = randomLongPnc(),
            ),
          ),
        ),
      )

      awaitNotNull { personRepository.findByDefendantId(defendantId) }

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
      )
      val sqsMessage =
        rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_COURT_PERSON_CREATED))
      val domainEvent: CprPersonCreated = jsonMapper.readValue<CprPersonCreated>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_COURT_PERSON_CREATED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/commonplatform/$defendantId")
      assertThat(domainEvent.description).isEqualTo("A court person record has been created")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("DEFENDANT_ID")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(defendantId)
    }

    @Test
    fun `should publish a CPR person created domain event when a person is created in libra`() {
      val cid = randomCId()

      publishLibraMessage(
        libraHearing(
          cId = cid,
          firstName = randomName(),
          lastName = randomName(),
          defendantType = PERSON,
        ),
      )

      awaitNotNull { personRepository.findByCId(cid) }

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
      )
      val sqsMessage =
        rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_COURT_PERSON_CREATED))
      val domainEvent: CprPersonCreated = jsonMapper.readValue<CprPersonCreated>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_COURT_PERSON_CREATED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/libra/$cid")
      assertThat(domainEvent.description).isEqualTo("A court person record has been created")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("C_ID")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(cid)
    }
  }

  @Nested
  inner class PersonUpdatedScenarios {

    @Test
    fun `should publish a CPR person updated domain event when delius person data changes`() {
      val crn = randomCrn()
      val personDetails = createRandomProbationCase(crn)

      probationCreateEventAndResponseSetup(ApiResponseSetup.from(personDetails.copy(ethnicity = Value(EthnicityCode.A1.name))))
      awaitNotNull { personRepository.findByCrn(crn) }
      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

      probationUpdateEventAndResponseSetup(ApiResponseSetup.from(personDetails.copy(ethnicity = Value(EthnicityCode.A2.name))))
      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
      )
      val sqsMessage =
        rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_UPDATED))
      val domainEvent: CprPersonUpdated = jsonMapper.readValue<CprPersonUpdated>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_UPDATED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn")
      assertThat(domainEvent.description).isEqualTo("A probation person record has been updated")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("CRN")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(crn)
    }

    @Test
    fun `should not publish a CPR person update domain event when no delius person data changes`() {
      val crn = randomCrn()
      val personDetails = createRandomProbationCase(crn)

      probationCreateEventAndResponseSetup(ApiResponseSetup.from(personDetails))
      awaitNotNull { personRepository.findByCrn(crn) }
      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

      probationUpdateEventAndResponseSetup(ApiResponseSetup.from(personDetails))
      expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
    }
  }

  @Test
  fun `should not publish a CPR person deleted domain event when a person is deleted in nomis`() {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(PrisonPersonCreated(personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber)))))
    val personCreated = awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )

    stubDeletePersonMatch()
    webTestClient.delete()
      .uri("/person/prison/$prisonNumber")
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk

    checkTelemetry(
      event = TelemetryEventType.CPR_RECORD_DELETED,
      expected = mapOf(
        "UUID" to personCreated.personKey!!.personUUID.toString(),
        EventKeys.IS_OVERRIDE_MARKER_DELETE.name to "false",
      ),
    )

    checkEventLogExist(
      sourceSystemId = personCreated.prisonNumber!!,
      event = CPRLogEvents.CPR_RECORD_DELETED,
    )

    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }

  @Test
  fun `should publish a CPR person deleted domain event when a person is deleted in delius`() {
    val crn = randomCrn()
    probationCreateEventAndResponseSetup(ApiResponseSetup.from(createRandomProbationCase(crn)))
    awaitNotNull { personRepository.findByCrn(crn) }
    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )

    stubDeletePersonMatch()
    publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETED, crn)

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_DELETED))
    val domainEvent: CprPersonDeleted = jsonMapper.readValue<CprPersonDeleted>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_DELETED)
    assertThat(domainEvent.description).isEqualTo("A probation person record has been deleted")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("CRN")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(crn)
  }
}
