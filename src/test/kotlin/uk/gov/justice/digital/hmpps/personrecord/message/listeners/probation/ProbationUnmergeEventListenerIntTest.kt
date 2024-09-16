package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

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
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.Duration
class ProbationUnmergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  private fun probationUrl(crn: String) = "/probation-cases/$crn"

  @Test
  fun `processes offender unmerge event is published`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    val source = ApiResponseSetup(crn = sourceCrn)
    val target = ApiResponseSetup(crn = targetCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, source, target)

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }

  @Test
  fun `should retry on 500 error`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    stub500Response(probationUrl(targetCrn), "next request will succeed", "retry")

    val source = ApiResponseSetup(crn = sourceCrn)
    val target = ApiResponseSetup(crn = targetCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, source, target, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )

    //  Add check to make sure record updated once added CPR-338
  }

  @Test
  fun `should log when message processing fails`() {
    val sourceCrn = randomCRN()
    val targetCrn = randomCRN()
    stub500Response(probationUrl(targetCrn), STARTED, "failure")
    stub500Response(probationUrl(targetCrn), STARTED, "failure")
    stub500Response(probationUrl(targetCrn), STARTED, "failure")

    val messageId = publishDomainEvent(
      OFFENDER_UNMERGED,
      DomainEvent(
        eventType = OFFENDER_UNMERGED,
        additionalInformation = AdditionalInformation(
          sourceCrn = sourceCrn,
          targetCrn = targetCrn,
        ),
      ),
    )

    probationMergeEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationMergeEventsQueue!!.queueUrl).build(),
    ).get()
    probationMergeEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(probationMergeEventsQueue!!.dlqUrl).build(),
    ).get()

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_CRN" to sourceCrn, "TARGET_CRN" to targetCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "SOURCE_SYSTEM" to "DELIUS",
        EventKeys.EVENT_TYPE.toString() to OFFENDER_UNMERGED,
        EventKeys.MESSAGE_ID.toString() to messageId,
      ),
    )
  }
}
