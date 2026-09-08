package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
@Profile("preprod | prod")
class PrisonEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val prisonEventProcessor: PrisonEventProcessor,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @SqsListener(Queues.PRISON_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEventDev(rawMessage: String) = domainEventProcessor.process<DomainEvent>(rawMessage) { event ->
    when (event) {
      is PrisonPersonCreated -> processPrisonEvent(event.prisonNumber)
      is PrisonPersonUpdated -> processPrisonEvent(event.prisonNumber)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  private fun processPrisonEvent(prisonNumber: String) {
    prisonerSearchClient.getPrisoner(prisonNumber)?.let { person ->
      prisonEventProcessor.processEvent(person)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Component
@Profile("!preprod & !prod")
class PrisonEventListenerDev(
  private val domainEventProcessor: DomainEventProcessor,
  private val prisonEventProcessor: PrisonEventProcessor,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @SqsListener(Queues.PRISON_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEventDev(rawMessage: String) = domainEventProcessor.process<DomainEvent>(rawMessage) { event ->
    when (event) {
      is PrisonPersonCreated -> processPrisonEvent(event.prisonNumber)
      is PrisonPersonUpdated -> processPrisonEvent(event.prisonNumber)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  private fun processPrisonEvent(prisonNumber: String) {
    prisonerSearchClient.getPrisoner(prisonNumber)?.let { person ->
      prisonEventProcessor.processEvent(person, setOf(PseudonymEntity::class))
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
