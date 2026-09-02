package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.libra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.MessageAttribute
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class LibraPersonDomainEventPublisherIntTest : MessagingTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should publish CPR person domain create event from libra person created`() {
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

  @Test
  fun `should publish CPR person domain update event from libra person updated`() {
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
    purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

    publishLibraMessage(
      libraHearing(
        cId = cid,
        firstName = randomName(),
        lastName = randomName(),
        defendantType = PERSON,
      ),
    )

    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage =
      rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_COURT_PERSON_UPDATED))
    val domainEvent: CprPersonUpdated = jsonMapper.readValue<CprPersonUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_COURT_PERSON_UPDATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/libra/$cid")
    assertThat(domainEvent.description).isEqualTo("A court person record has been updated")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("C_ID")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(cid)
  }
}

@Nested
@ActiveProfiles("preprod")
class PreProd : MessagingTestBase() {
  @Test
  fun `should not publish CPR person domain events from libra messages`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
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
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.LIBRA.name, "C_ID" to cid),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    publishLibraMessage(
      libraHearing(
        cId = cid,
        firstName = randomName(),
        lastName = randomName(),
        defendantType = PERSON,
      ),
    )

    awaitNotNull { personRepository.findByCId(cid) }
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.LIBRA.name, "C_ID" to cid),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }
}

@Nested
@ActiveProfiles("prod")
class Prod : MessagingTestBase() {
  @Test
  fun `should not publish CPR person domain events from libra messages`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
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
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.LIBRA.name, "C_ID" to cid),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

    publishLibraMessage(
      libraHearing(
        cId = cid,
        firstName = randomName(),
        lastName = randomName(),
        defendantType = PERSON,
      ),
    )

    awaitNotNull { personRepository.findByCId(cid) }
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to SourceSystemType.LIBRA.name, "C_ID" to cid),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }
}
