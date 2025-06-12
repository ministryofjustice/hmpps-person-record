package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService

@Component
@Profile("!seeding")
class PrisonMergeEventListener(
  private val sqsListenerService: SQSListenerService,
  private val mergeEventProcessor: PrisonMergeEventProcessor,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener(Queues.PRISON_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processSQSMessage(rawMessage) {
    mergeEventProcessor.processEvent(objectMapper.readValue<DomainEvent>(it.message))
  }
}
