package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.message.processor.CourtCaseEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_PROCESSING_FAILURE

const val CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY = "cprcourtcaseeventsqueue"

@Component
class CourtCaseEventsListener(
  val objectMapper: ObjectMapper,
  val courtCaseEventsProcessor: CourtCaseEventsProcessor,
  val telemetryService: TelemetryService,
  val featureFlag: FeatureFlag,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_court_case_events_queue", kind = SpanKind.SERVER)
  fun onMessage(
    rawMessage: String,
  ) {
    if (featureFlag.isHmctsSQSEnabled()) {
      val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
      when (sqsMessage.type) {
        "Notification" -> {
          try {
            courtCaseEventsProcessor.processEvent(sqsMessage)
          } catch (e: Exception) {
            LOG.error("Failed to process message:${sqsMessage.messageId}", e)
            telemetryService.trackEvent(
              HMCTS_PROCESSING_FAILURE,
              mapOf("MESSAGE_ID" to sqsMessage.messageId),
            )
            throw e
          }
        }
        else -> {
          LOG.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
        }
      }
    } else {
      LOG.debug("Message processing is switched off")
    }
  }
}
