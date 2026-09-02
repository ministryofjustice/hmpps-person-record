package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PrisonPersonDomainEventPublisherIntTest : MessagingTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should publish CPR person domain create event from prison person created`() {
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
  fun `should publish CPR person domain update event from prison person updated`() {
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
    purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(
      PrisonPersonUpdated(
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

    expectOneMessageOn(testOnlyCPRDomainEventsQueue)
    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage =
      rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_PRISON_PERSON_UPDATED))
    val domainEvent: CprPersonUpdated = jsonMapper.readValue<CprPersonUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_PRISON_PERSON_UPDATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/prison/$prisonNumber")
    assertThat(domainEvent.description).isEqualTo("A prison person record has been updated")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("prisonNumber")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(prisonNumber)
  }
}

@Nested
@ActiveProfiles("preprod")
class PreProd : MessagingTestBase() {
  @Test
  fun `should not publish CPR person domain events from nomis messages`() {
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
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(
      PrisonPersonUpdated(
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
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }
}

@Nested
@ActiveProfiles("prod")
class Prod : MessagingTestBase() {
  @Test
  fun `should not publish CPR person domain events from nomis messages`() {
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
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(
      PrisonPersonUpdated(
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
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }
}
