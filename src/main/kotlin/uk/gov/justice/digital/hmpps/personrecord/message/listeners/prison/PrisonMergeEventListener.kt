package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService

@Component
class PrisonMergeEventListener(
  private val sqsListenerService: SQSListenerService,
  private val mergeEventProcessor: PrisonMergeEventProcessor,
) {

  @SqsListener(Queues.PRISON_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processDomainEvent(rawMessage) {
    mergeEventProcessor.processEvent(it)
  }
}
