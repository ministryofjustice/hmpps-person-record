package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PersonDomainEventPublisherIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should publish a CPR person created domain event when a person is created in delius`() {
    val crn = randomCrn()

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(createRandomProbationCase(crn)))

    awaitNotNull {
      personRepository.findByCrn(crn)
    }

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
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
