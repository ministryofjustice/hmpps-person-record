package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
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
    val unmergedCrn = domainEvent.additionalInformation?.unmergedCrn!!
    val existingPerson = corePersonRecordAndDeliusClient
      .getPerson(unmergedCrn)
      .let {
        createUpdateService.processPerson(
          it,
        ) { personRepository.findByCrn(unmergedCrn) }
      }

    val reactivatedCrn = domainEvent.additionalInformation.reactivatedCrn!!
    val reactivatedPerson = corePersonRecordAndDeliusClient
      .getPerson(reactivatedCrn)
      .let {
        createUpdateService.processPerson(
          it.doNotLinkOnCreate(),
        ) { personRepository.findByCrn(reactivatedCrn) }
      }

    unmergeService.processUnmerge(reactivatedPerson, existingPerson)
  }
}
