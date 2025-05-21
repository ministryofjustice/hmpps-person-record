package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import feign.FeignException
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationUnmergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService.Companion.whenEvent
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED

@Component
@Profile("!seeding")
class ProbationMergeEventListener(
  private val sqsListenerService: SQSListenerService,
  private val mergeEventProcessor: ProbationMergeEventProcessor,
  private val unmergeEventProcessor: ProbationUnmergeEventProcessor,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(Queues.PROBATION_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processDomainEvent(rawMessage) { domainEvent ->
    domainEvent
      .whenEvent(OFFENDER_MERGED) { handleMergeEvent(it) }
      .whenEvent(OFFENDER_UNMERGED) { handleUnmergeEvent(it) }
  }

  private fun handleMergeEvent(domainEvent: DomainEvent) {
    try {
      mergeEventProcessor.processEvent(domainEvent)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding merge message for status code: ${e.status()}")
    }
  }

  private fun handleUnmergeEvent(domainEvent: DomainEvent) {
    try {
      unmergeEventProcessor.processEvent(domainEvent)
    } catch (e: FeignException.NotFound) {
      log.info("Discarding unmerge message for status code: ${e.status()}")
    }
  }
}
