package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.message.processors.hmcts.CourtCaseEventsProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.MESSAGE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

const val CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY = "cprcourtcaseeventsqueue"

@Component
class CourtCaseEventsListener(
  val objectMapper: ObjectMapper,
  val courtCaseEventsProcessor: CourtCaseEventsProcessor,
  val telemetryService: TelemetryService,
  val featureFlag: FeatureFlag,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_court_case_events_queue", kind = SERVER)
  fun onMessage(
    rawMessage: String,
  ) {
    if (featureFlag.isHmctsSQSEnabled()) {
      val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
      when (sqsMessage.type) {
        NOTIFICATION -> {
          try {
            courtCaseEventsProcessor.processEvent(sqsMessage)
          } catch (e: Exception) {
            telemetryService.trackEvent(
              MESSAGE_PROCESSING_FAILED,
              mapOf(
                MESSAGE_ID to sqsMessage.messageId,
                SOURCE_SYSTEM to SourceSystemType.HMCTS.name,
              ),
            )
            throw e
          }
        }
        else -> {
          log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
        }
      }
    } else {
      log.debug("Message processing is switched off")
    }
  }
}
