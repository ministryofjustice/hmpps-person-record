package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PersonDomainEventPublisherE2ETest : E2ETestBase() {

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

  @Nested
  inner class PersonMerged {
    @Test
    fun `should publish a CPR person merged domain event when a nomis person is merged`() {
      val fromPrisonNumber = randomPrisonNumber()
      val toPrisonNumber = randomPrisonNumber()

      stubPrisonResponse(ApiResponseSetup(prisonNumber = fromPrisonNumber))
      publishDomainEvent(
        PrisonPersonCreated(
          personReference = PersonReference(
            listOf(
              PersonIdentifier(
                "NOMS",
                fromPrisonNumber,
              ),
            ),
          ),
        ),
      )

      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))
      publishDomainEvent(
        PrisonPersonCreated(
          personReference = PersonReference(
            listOf(
              PersonIdentifier(
                "NOMS",
                toPrisonNumber,
              ),
            ),
          ),
        ),
      )

      awaitNotNull { personRepository.findByPrisonNumber(fromPrisonNumber) }
      awaitNotNull { personRepository.findByPrisonNumber(toPrisonNumber) }
      purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))
      sendPostRequestAsserted<Unit>(
        url = "/syscon-sync/person/$toPrisonNumber/merge",
        body = PrisonMerge(fromPrisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NO_CONTENT,
      )

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val sqsMessage = receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue)
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_PERSON_MERGED))
      val domainEvent = jsonMapper.readValue<CprPersonMerged>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_PERSON_MERGED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$toPrisonNumber")
      assertThat(domainEvent.description).isEqualTo("A prison person record has been merged")
      assertThat(domainEvent.occurredAt).isNotNull()
      assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(2)
      assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("fromPrisonNumber")
      assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(fromPrisonNumber)
      assertThat(domainEvent.personReference.identifiers?.get(1)?.type).isEqualTo("toPrisonNumber")
      assertThat(domainEvent.personReference.identifiers?.get(1)?.value).isEqualTo(toPrisonNumber)
    }

    @Test
    fun `should publish a CPR person updated domain event when a delius person is merged`() {
      // to be replaced with a check for the cpr probation person merged event once SAS are ready for it
      val fromCrn = randomCrn()
      val toCrn = randomCrn()

      probationCreateEventAndResponseSetup(ApiResponseSetup(crn = fromCrn))
      probationCreateEventAndResponseSetup(ApiResponseSetup(crn = toCrn))

      awaitNotNull { personRepository.findByCrn(fromCrn) }
      awaitNotNull { personRepository.findByCrn(toCrn) }
      purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

      probationMergeEventAndResponseSetup(fromCrn, toCrn)

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val sqsMessage = receiveNextMessageOnQueue(testOnlyCPRDomainEventsQueue)
      assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_UPDATED))
      val domainEvent = jsonMapper.readValue<CprPersonUpdated>(sqsMessage.message)
      assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_UPDATED)
      assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$toCrn")
    }

    @Test
    fun `should not publish a CPR person merged domain event when a nomis person is merged without a from person`() {
      val toPrisonNumber = randomPrisonNumber()
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))
      publishDomainEvent(
        PrisonPersonCreated(
          personReference = PersonReference(
            listOf(
              PersonIdentifier(
                "NOMS",
                toPrisonNumber,
              ),
            ),
          ),
        ),
      )

      awaitNotNull { personRepository.findByPrisonNumber(toPrisonNumber) }
      purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))
      sendPostRequestAsserted<Unit>(
        url = "/syscon-sync/person/$toPrisonNumber/merge",
        body = PrisonMerge(randomPrisonNumber()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR,
      )

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
