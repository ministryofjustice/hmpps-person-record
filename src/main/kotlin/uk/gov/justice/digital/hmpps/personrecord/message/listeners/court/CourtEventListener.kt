package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.CommonPlatformEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.court.LibraEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService

@Component
@ConditionalOnProperty(
  name = ["sqs.listeners.enabled"],
  havingValue = "true",
  matchIfMissing = true,
)
class CourtEventListener(
  private val commonPlatformEventProcessor: CommonPlatformEventProcessor,
  private val libraEventProcessor: LibraEventProcessor,
  private val sqsListenerService: SQSListenerService,
) {

  @SqsListener(Queues.COURT_CASES_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) = sqsListenerService.processSQSMessage(rawMessage) { sqsMessage ->
    when (sqsMessage.getMessageType()) {
      COMMON_PLATFORM_HEARING.name -> commonPlatformEventProcessor.processEvent(sqsMessage)
      LIBRA_COURT_CASE.name -> libraEventProcessor.processEvent(sqsMessage)
    }
  }
}
