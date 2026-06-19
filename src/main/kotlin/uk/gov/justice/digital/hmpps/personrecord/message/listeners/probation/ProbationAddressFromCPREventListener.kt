package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_ADDRESS_EVENT_FROM_CPR_QUEUE

@Component
class ProbationAddressFromCPREventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val deliusAddressIdHandler: DeliusAddressIdHandler,
) {

  @SqsListener(PROBATION_ADDRESS_EVENT_FROM_CPR_QUEUE, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process(rawMessage) { event ->
    when (event) {
      is ProbationOffenderAddressCreated -> deliusAddressIdHandler.patchAddress(event)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
