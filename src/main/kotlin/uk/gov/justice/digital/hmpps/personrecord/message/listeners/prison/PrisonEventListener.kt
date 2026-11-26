package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxy
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
class PrisonEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val personService: PrisonPersonServiceProxy,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @SqsListener(Queues.PRISON_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process<DomainEvent>(rawMessage) { event ->
    when (event) {
      is PrisonPersonCreated -> processPrisonEventCreated(event.prisonNumber)
      is PrisonPersonUpdated -> processPrisonEventUpdated(event.prisonNumber)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  private fun processPrisonEventCreated(prisonNumber: String) {
    prisonerSearchClient.getPrisoner(prisonNumber)?.let { person ->
      logFieldsNotCoveredBySingleAlias(prisonNumber, person)
      personService.processPersonCreate(person)
    }
  }

  private fun processPrisonEventUpdated(prisonNumber: String) {
    prisonerSearchClient.getPrisoner(prisonNumber)?.let { person ->
      personService.processPersonUpdate(person)
    }
  }

  private fun logFieldsNotCoveredBySingleAlias(prisonNumber: String, person: Person) {
    val extraFields = fieldsNotCoveredBySingleAlias(person)
    if (extraFields.isNotEmpty()) {
      log.info(
        "Prison person created event returned more information for $prisonNumber than a single alias with no identifiers: ${extraFields.joinToString()}",
      )
    } else {
      log.info("Prison person created event returned no more information for $prisonNumber than a single alias with no identifiers")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    internal fun fieldsNotCoveredBySingleAlias(person: Person): List<String> = listOfNotNull(
      "aliases".takeIf { person.aliases.isNotEmpty() },
      "nationalities".takeIf { person.nationalities.isNotEmpty() },
      "contacts".takeIf { person.contacts.isNotEmpty() },
      "addresses".takeIf { person.addresses.isNotEmpty() },
      "references".takeIf { person.references.isNotEmpty() },
      "sentences".takeIf { person.sentences.isNotEmpty() },
    )
  }
}
