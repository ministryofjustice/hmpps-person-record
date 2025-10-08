package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val personService: PersonService,
) {

  @Transactional
  fun processEvent(domainEvent: DomainEvent) {
    val prisonNumber = domainEvent.getPrisonNumber()
    prisonerSearchClient.getPrisoner(prisonNumber)?.let {
      val from: PersonEntity? =
        personRepository.findByPrisonNumber(domainEvent.additionalInformation?.sourcePrisonNumber!!)
      val to: PersonEntity = personService.processPerson(
        it.doNotReclusterOnUpdate(),
      ) { personRepository.findByPrisonNumber(prisonNumber) }
      mergeService.processMerge(from, to)
    }
  }
}
