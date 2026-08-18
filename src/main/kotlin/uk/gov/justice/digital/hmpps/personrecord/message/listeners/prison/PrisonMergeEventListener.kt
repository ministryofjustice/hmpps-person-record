package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
class PrisonMergeEventListener(
  private val domainEventProcessor: DomainEventProcessor,
) {

  @SqsListener(Queues.PRISON_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process<PrisonPersonMerged>(rawMessage) {
    log.info("Ignoring merge event: $it")
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
