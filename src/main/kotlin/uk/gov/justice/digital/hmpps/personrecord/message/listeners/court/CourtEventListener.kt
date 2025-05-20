package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.NOTIFICATION
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.CourtEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
@Profile("!seeding")
class CourtEventListener(
  private val objectMapper: ObjectMapper,
  private val courtEventProcessor: CourtEventProcessor,
) {

  @SqsListener(Queues.COURT_CASES_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) = TimeoutExecutor.runWithTimeout {
    val sqsMessage = objectMapper.readValue<SQSMessage>(rawMessage)
    when (sqsMessage.type) {
      NOTIFICATION -> courtEventProcessor.processEvent(sqsMessage)
    }
  }
}
