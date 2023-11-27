package uk.gov.justice.digital.hmpps.personrecord.service.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.service.CourtCaseEventsProcessor

const val CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY = "cprcourtcaseeventsqueue"

@Component
class CourtCaseEventsListener(
  val objectMapper: ObjectMapper,
  val courtCaseEventsProcessor: CourtCaseEventsProcessor,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(CPR_COURT_CASE_EVENTS_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    LOG.debug("Enter onMessage")
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    LOG.debug("Received message: type:${sqsMessage.type} message:${sqsMessage.message}")
    when (sqsMessage.type) {
      "Notification" -> {
        try {
          courtCaseEventsProcessor.processEvent(sqsMessage)
        } catch (e: Exception) {
          LOG.error("Failed to process message:${sqsMessage.messageId}", e)
          throw e
        }
      }
      else -> {
        LOG.info("Received a message I wasn't expecting Type: ${sqsMessage.type}")
      }
    }
  }
}
