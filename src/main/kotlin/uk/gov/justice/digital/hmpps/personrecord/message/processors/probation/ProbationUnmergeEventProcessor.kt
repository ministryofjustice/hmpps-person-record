package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

@Component
class ProbationUnmergeEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val unmergeService: UnmergeService,
  private val personRepository: PersonRepository,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  fun processEvent(domainEvent: DomainEvent) {
    val existingPerson = getProbationPerson(domainEvent.additionalInformation?.unmergedCrn!!, true)
    val reactivatedPerson = getProbationPerson(domainEvent.additionalInformation.reactivatedCrn!!, false)
    unmergeService.processUnmerge(reactivatedPerson, existingPerson)
  }

  private fun getProbationPerson(crn: String, shouldLinkOnCreate: Boolean): PersonEntity = corePersonRecordAndDeliusClient
    .getPerson(crn)
    .let {
      createUpdateService.processPerson(
        it,
        shouldLinkOnCreate = shouldLinkOnCreate,
      ) { personRepository.findByCrn(crn) }
    }
}
