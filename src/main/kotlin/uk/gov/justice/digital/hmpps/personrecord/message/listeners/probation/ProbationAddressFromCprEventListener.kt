package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_ADDRESS_EVENT_FROM_CPR_QUEUE

@Component
class ProbationAddressFromCprEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val deliusAddressIdHandler: DeliusAddressIdHandler,
) {

  @SqsListener(PROBATION_ADDRESS_EVENT_FROM_CPR_QUEUE, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processHmppsDomainEvent<ProbationOffenderAddressCreated>(rawMessage) { event ->
      deliusAddressIdHandler.patchAddress(event)
  }
}
