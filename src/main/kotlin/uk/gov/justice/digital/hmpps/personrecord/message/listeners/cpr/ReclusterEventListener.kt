package uk.gov.justice.digital.hmpps.personrecord.message.listeners.cpr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.Recluster
import uk.gov.justice.digital.hmpps.personrecord.message.processors.cpr.ReclusterEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.type.RECLUSTER_EVENT
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

@Component
@Profile("!seeding")
class ReclusterEventListener(
  private val objectMapper: ObjectMapper,
  private val reclusterEventProcessor: ReclusterEventProcessor,
  private val telemetryService: TelemetryService,
) {

  @SqsListener(Queues.RECLUSTER_EVENTS_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) = TimeoutExecutor.runWithTimeout {
    val reclusterEvent = objectMapper.readValue<Recluster>(rawMessage)
    handleEvent(reclusterEvent)
  }

  private fun handleEvent(reclusterEvent: Recluster) {
    try {
      reclusterEventProcessor.processEvent(reclusterEvent)
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          EVENT_TYPE to RECLUSTER_EVENT,
        ),
      )
      throw e
    }
  }
}
