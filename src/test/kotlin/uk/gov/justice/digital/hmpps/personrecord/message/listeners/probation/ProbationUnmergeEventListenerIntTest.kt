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
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_SELF_MATCH
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
  fun `offender unmerge event is published`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged)

    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_SELF_MATCH,
      mapOf(
        "IS_ABOVE_SELF_MATCH_THRESHOLD" to "true",
        "PROBABILITY_SCORE" to "0.9999",
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
        "CRN" to reactivatedCrn,
      ),
    )
    checkTelemetry(
      CPR_SELF_MATCH,
      mapOf(
        "IS_ABOVE_SELF_MATCH_THRESHOLD" to "true",
        "PROBABILITY_SCORE" to "0.9999",
        "SOURCE_SYSTEM" to SourceSystemType.DELIUS.name,
        "CRN" to unmergedCrn,
      ),
    )
  }

  @Test
  fun `should retry on 500 error`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    stub500Response(probationUrl(unmergedCrn), "next request will succeed", "retry")

    val reactivated = ApiResponseSetup(crn = reactivatedCrn)
    val unmerged = ApiResponseSetup(crn = unmergedCrn)
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivated, unmerged, scenario = "retry", currentScenarioState = "next request will succeed")

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await.atMost(Duration.ofSeconds(2)) untilCallTo {
      probationMergeEventsQueue?.sqsDlqClient?.countAllMessagesOnQueue(probationMergeEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }
    checkTelemetry(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
    )
  }

  @Test
  fun `should log when message processing fails`() {
    val reactivatedCrn = randomCRN()
    val unmergedCrn = randomCRN()
    stub500Response(probationUrl(unmergedCrn), STARTED, "failure")
    stub500Response(probationUrl(unmergedCrn), STARTED, "failure")
    stub500Response(probationUrl(unmergedCrn), STARTED, "failure")

    val messageId = publishDomainEvent(
      OFFENDER_UNMERGED,
      DomainEvent(
        eventType = OFFENDER_UNMERGED,
        additionalInformation = AdditionalInformation(
          reactivatedCRN = reactivatedCrn,
          unmergedCRN = unmergedCrn,
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
      mapOf("REACTIVATED_CRN" to reactivatedCrn, "UNMERGED_CRN" to unmergedCrn, "EVENT_TYPE" to OFFENDER_UNMERGED, "SOURCE_SYSTEM" to "DELIUS"),
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
