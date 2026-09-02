package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationPersonDomainEventPublisherIntTest : MessagingTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
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
    Assertions.assertThat(sqsMessage.messageAttributes?.eventType)
      .isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_CREATED))
    val domainEvent: CprPersonCreated = jsonMapper.readValue<CprPersonCreated>(sqsMessage.message)
    Assertions.assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_CREATED)
    Assertions.assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn")
    Assertions.assertThat(domainEvent.description).isEqualTo("A probation person record has been created")
    Assertions.assertThat(domainEvent.occurredAt).isNotNull()
    Assertions.assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    Assertions.assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("CRN")
    Assertions.assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(crn)
  }

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
    Assertions.assertThat(sqsMessage.messageAttributes?.eventType)
      .isEqualTo(MessageAttribute(CPR_PROBATION_PERSON_UPDATED))
    val domainEvent: CprPersonUpdated = jsonMapper.readValue<CprPersonUpdated>(sqsMessage.message)
    Assertions.assertThat(domainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_UPDATED)
    Assertions.assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/probation/$crn")
    Assertions.assertThat(domainEvent.description).isEqualTo("A probation person record has been updated")
    Assertions.assertThat(domainEvent.occurredAt).isNotNull()
    Assertions.assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    Assertions.assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("CRN")
    Assertions.assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(crn)
  }

  @Test
  fun `should not publish a CPR person updated domain event when no delius person data changes`() {
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
