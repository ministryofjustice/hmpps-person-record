package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationUnmergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED

@Component
class ProbationMergeEventListener(
  private val sqsListenerService: SQSListenerService,
  private val mergeEventProcessor: ProbationMergeEventProcessor,
  private val unmergeEventProcessor: ProbationUnmergeEventProcessor,
) {

  @SqsListener(Queues.PROBATION_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processDomainEvent(rawMessage) {
    it
      .whenEvent(OFFENDER_MERGED) { event -> mergeEventProcessor.processEvent(event) }
      .whenEvent(OFFENDER_UNMERGED) { event -> unmergeEventProcessor.processEvent(event) }
  }
}

private fun DomainEvent.whenEvent(eventType: String, action: (domainEvent: DomainEvent) -> Unit): DomainEvent {
  when {
    this.eventType == eventType -> action(this)
  }
  return this
}
