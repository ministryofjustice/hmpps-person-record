package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

@Component
class PrisonMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val createUpdateService: CreateUpdateService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    prisonerSearchClient.getPrisoner(domainEvent.additionalInformation?.prisonNumber!!)?.let {
      val from: PersonEntity? =
        personRepository.findByPrisonNumber(domainEvent.additionalInformation.sourcePrisonNumber!!)
      val to: PersonEntity = createUpdateService.processPerson(
        Person.from(it),
        shouldReclusterOnUpdate = false,
      ) { personRepository.findByPrisonNumber(domainEvent.additionalInformation.prisonNumber) }
      mergeService.processMerge(from, to)

      to.personKey?.let { toKey -> personKeyRepository.save(toKey) }

      personRepository.save(to)
      from?.let { fromRecord -> personRepository.save(fromRecord) }
    }
  }
}
