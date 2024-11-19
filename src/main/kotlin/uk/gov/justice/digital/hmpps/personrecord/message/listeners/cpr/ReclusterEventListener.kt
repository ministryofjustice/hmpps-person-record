package uk.gov.justice.digital.hmpps.personrecord.message.listeners.cpr

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.Recluster
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.CourtEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.cpr.ReclusterEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.RECLUSTER_EVENT
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

private const val RECLUSTER_EVENTS_QUEUE_CONFIG_KEY = "cprreclustereventsqueue"

@Component
@Profile("!seeding")
class ReclusterEventListener(
  val objectMapper: ObjectMapper,
  val reclusterEventProcessor: ReclusterEventProcessor,
  val telemetryService: TelemetryService,
) {

  @SqsListener(RECLUSTER_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) {
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
