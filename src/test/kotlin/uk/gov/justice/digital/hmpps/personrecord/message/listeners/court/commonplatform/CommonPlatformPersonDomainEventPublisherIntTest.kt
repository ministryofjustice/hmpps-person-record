package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

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
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc

class CommonPlatformPersonDomainEventPublisherIntTest : MessagingTestBase() {

  @BeforeEach
  fun setup() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should publish CPR person domain create event from common platform person created`() {
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
  fun `should publish CPR person domain update event from common platform person updated`() {
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
    purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)

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

    val rawDomainEventMessage = testOnlyCPRDomainEventsQueue?.sqsClient?.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(testOnlyCPRDomainEventsQueue?.queueUrl).build(),
    )
    val sqsMessage =
      rawDomainEventMessage?.get()?.messages()?.first()?.let { jsonMapper.readValue<SQSMessage>(it.body()) }!!
    assertThat(sqsMessage.messageAttributes?.eventType).isEqualTo(MessageAttribute(CPR_COURT_PERSON_UPDATED))
    val domainEvent: CprPersonUpdated = jsonMapper.readValue<CprPersonUpdated>(sqsMessage.message)
    assertThat(domainEvent.eventType).isEqualTo(CPR_COURT_PERSON_UPDATED)
    assertThat(domainEvent.detailUrl).isEqualTo("http://localhost:8080/person/commonplatform/$defendantId")
    assertThat(domainEvent.description).isEqualTo("A court person record has been updated")
    assertThat(domainEvent.occurredAt).isNotNull()
    assertThat(domainEvent.personReference.identifiers?.size).isEqualTo(1)
    assertThat(domainEvent.personReference.identifiers?.get(0)?.type).isEqualTo("DEFENDANT_ID")
    assertThat(domainEvent.personReference.identifiers?.get(0)?.value).isEqualTo(defendantId)
  }
}

@Nested
@ActiveProfiles("preprod")
class PreProd : MessagingTestBase() {

  @Test
  fun `should not publish CPR person domain events from common platform messages`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
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
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to COMMON_PLATFORM.name, "DEFENDANT_ID" to defendantId),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

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
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to COMMON_PLATFORM.name, "DEFENDANT_ID" to defendantId),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }
}

@Nested
@ActiveProfiles("prod")
class Prod : MessagingTestBase() {
  @Test
  fun `should not publish CPR person domain events from common platform messages`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
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
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to COMMON_PLATFORM.name, "DEFENDANT_ID" to defendantId),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)

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
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to COMMON_PLATFORM.name, "DEFENDANT_ID" to defendantId),
    )
    expectNoMessagesOn(testOnlyCPRDomainEventsQueue)
  }
}
