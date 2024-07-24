package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.CourtEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.MESSAGE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

const val CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY = "cprcourtcaseeventsqueue"

@Component
@Profile("!seeding")
class CourtEventListener(
  val objectMapper: ObjectMapper,
  val courtEventProcessor: CourtEventProcessor,
  val telemetryService: TelemetryService,
  val applicationContext: ApplicationContext,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-person-record-cpr_court_case_events_queue", kind = SERVER)
  fun onMessage(
    rawMessage: String,
  ) {
    log.warn("processed by ApplicationContext ${applicationContext.id}")

    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> handleEvent(sqsMessage)
      else -> {
        log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
      }
    }
  }

  fun handleEvent(sqsMessage: SQSMessage) {
    try {
      courtEventProcessor.processEvent(sqsMessage)
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          SOURCE_SYSTEM to SourceSystemType.COMMON_PLATFORM.name,
          EVENT_TYPE to sqsMessage.getMessageType(),
          MESSAGE_ID to sqsMessage.messageId,
        ),
      )
      throw e
    }
  }
}
