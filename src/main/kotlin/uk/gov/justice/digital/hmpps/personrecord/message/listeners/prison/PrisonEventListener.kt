package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED

const val PRISON_EVENT_QUEUE_CONFIG_KEY = "cprnomiseventsqueue"

@Component
@Profile("!seeding")
class PrisonEventListener(
  private val objectMapper: ObjectMapper,
  private val prisonEventProcessor: PrisonEventProcessor,
  private val telemetryService: TelemetryService,
  @Value("\${timeout.message}") private val messageTimeoutMs: Long = 90000,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISON_EVENT_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = TimeoutExecutor.runWithTimeout(messageTimeoutMs) {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> {
        val domainEvent = objectMapper.readValue<DomainEvent>(sqsMessage.message)
        handleEvent(domainEvent, sqsMessage.messageId)
      }
      else -> log.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
    }
  }

  private fun handleEvent(domainEvent: DomainEvent, messageId: String?) {
    try {
      prisonEventProcessor.processEvent(domainEvent)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding message for status code: ${e.status()}")
    } catch (e: Exception) {
      telemetryService.trackEvent(
        MESSAGE_PROCESSING_FAILED,
        mapOf(
          EventKeys.EVENT_TYPE to domainEvent.eventType,
          EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
          EventKeys.MESSAGE_ID to messageId,
        ),
      )
      throw e
    }
  }
}
