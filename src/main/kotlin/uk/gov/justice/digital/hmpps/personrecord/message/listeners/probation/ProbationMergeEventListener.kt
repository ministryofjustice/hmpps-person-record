package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationUnmergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
class ProbationMergeEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val mergeEventProcessor: ProbationMergeEventProcessor,
  private val unmergeEventProcessor: ProbationUnmergeEventProcessor,
) {

  @SqsListener(Queues.PROBATION_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process<DomainEvent>(rawMessage) { event ->
    when (event) {
      is ProbationPersonMerged -> mergeEventProcessor.processEvent(event)
      is ProbationPersonUnmerged -> unmergeEventProcessor.processEvent(event)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
