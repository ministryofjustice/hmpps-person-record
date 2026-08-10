package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionUpdateHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_RELIGION_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_RELIGION_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime

class ReligionDomainEventPublisherIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Autowired
  private lateinit var prisonReligionInsertHandler: PrisonReligionInsertHandler

  @Autowired
  private lateinit var prisonReligionUpdateHandler: PrisonReligionUpdateHandler

  @Test
  fun `should publish a CPR religion created domain event when a religion is created in nomis`() {
    val prisonNumber = randomPrisonNumber()
    createPerson(createRandomPrisonPersonDetails(prisonNumber))

    val cprReligionId = prisonReligionInsertHandler.handleInsert(prisonNumber, createPrisonReligionHistory()).cprReligionId

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_RELIGION_CREATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(NOMIS.identifier))
    val domainEvent: CprReligionCreated = jsonMapper.readValue<CprReligionCreated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_RELIGION_CREATED)
    assertThat(domainEvent.description).isEqualTo("A prison religion has been created for a person")
    assertThat(domainEvent.additionalInformation.cprReligionId.toString()).isEqualTo(cprReligionId)
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }

  @Test
  fun `should publish a CPR religion updated domain event when a religion is updated in nomis`() {
    val prisonNumber = randomPrisonNumber()
    val existingReligionEntity = prisonReligionRepository.saveAndFlush(PrisonReligionEntity.from(prisonNumber, createPrisonReligionHistory()))

    prisonReligionUpdateHandler.handleUpdate(
      existingReligionEntity.updateId.toString(),
      PrisonReligionUpdateRequest(
        modifyUserId = "A user",
        modifyDateTime = LocalDateTime.now(),
        comments = "Comments",
      ),
    )

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage = rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_RELIGION_UPDATED))
    assertThat(sqsMessage.messageAttributes?.eventSource).isEqualTo(MessageAttribute(NOMIS.identifier))
    val domainEvent: CprReligionUpdated = jsonMapper.readValue<CprReligionUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_RELIGION_UPDATED)
    assertThat(domainEvent.description).isEqualTo("A prison religion has been updated for a person")
    assertThat(domainEvent.additionalInformation.cprReligionId.toString()).isEqualTo(existingReligionEntity.updateId.toString())
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }
}
