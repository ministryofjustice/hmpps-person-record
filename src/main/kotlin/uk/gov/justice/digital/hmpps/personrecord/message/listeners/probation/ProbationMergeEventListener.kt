package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
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
class ProbationMergeEventListener(
  private val sqsListenerService: SQSListenerService,
  private val mergeEventProcessor: ProbationMergeEventProcessor,
  private val unmergeEventProcessor: ProbationUnmergeEventProcessor,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener(Queues.PROBATION_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processSQSMessage(rawMessage) {
    objectMapper.readValue<DomainEvent>(it.message)
      .whenEvent(OFFENDER_MERGED) { mergeEventProcessor.processEvent(it) }
      .whenEvent(OFFENDER_UNMERGED) { unmergeEventProcessor.processEvent(it) }
  }
}
