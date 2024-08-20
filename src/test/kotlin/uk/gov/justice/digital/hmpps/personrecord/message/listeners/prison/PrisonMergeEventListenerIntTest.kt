package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration

class PrisonMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  @Test
  fun `processes prisoner merge event is published`() {
    val sourcePrisonNumber = randomPrisonNumber()
    val targetPrisonNumber = randomPrisonNumber()
    val source = ApiResponseSetup(prisonNumber = sourcePrisonNumber)
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    prisonMergeEventAndResponseSetup(PRISONER_MERGED, source, target)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
  }

  @Test
  fun `should not push 404 to dead letter queue but discard message instead`() {
    val sourcePrisonNumber = randomPrisonNumber()
    val targetPrisonNumber = randomPrisonNumber()
    stub404Response(prisonURL(targetPrisonNumber))

    publishDomainEvent(
      PRISONER_MERGED,
      DomainEvent(
        eventType = PRISONER_MERGED,
        additionalInformation = AdditionalInformation(
          sourcePrisonNumber = sourcePrisonNumber,
          targetPrisonNumber = targetPrisonNumber,
        ),
      ),
    )

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(prisonMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(prisonMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
  }

  @Test
  fun `should retry on 500 error`() {
    val sourcePrisonNumber = randomPrisonNumber()
    val targetPrisonNumber = randomPrisonNumber()
    stub500Response(prisonURL(targetPrisonNumber), "next request will succeed", "retry")

    val source = ApiResponseSetup(prisonNumber = sourcePrisonNumber)
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    prisonMergeEventAndResponseSetup(PRISONER_MERGED, source, target, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(prisonMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      prisonMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(prisonMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to "NOMIS"),
    )
  }

  @Test
  fun `should log when message processing fails`() {
    val sourcePrisonNumber = randomPrisonNumber()
    val targetPrisonNumber = randomPrisonNumber()
    stub500Response(prisonURL(targetPrisonNumber), STARTED, "failure")
    stub500Response(prisonURL(targetPrisonNumber), STARTED, "failure")
    stub500Response(prisonURL(targetPrisonNumber), STARTED, "failure")

    val additionalInformation =
      AdditionalInformation(sourcePrisonNumber = sourcePrisonNumber, targetPrisonNumber = targetPrisonNumber)
    val domainEvent =
      DomainEvent(eventType = PRISONER_MERGED, personReference = null, additionalInformation = additionalInformation)
    val messageId = publishDomainEvent(PRISONER_MERGED, domainEvent)

    prisonMergeEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonMergeEventsQueue!!.queueUrl).build(),
    ).get()
    prisonMergeEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonMergeEventsQueue!!.dlqUrl).build(),
    ).get()
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "EVENT_TYPE" to PRISONER_MERGED,
        "SOURCE_SYSTEM" to "NOMIS",
      ),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to "NOMIS",
        "EVENT_TYPE" to "prisoner-offender-events.prisoner.merged",
      ),
    )
  }
}
