package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

@Component
class ProbationUnmergeEventProcessor(
  private val unmergeService: UnmergeService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val probationProcessor: ProbationProcessor,
) {

  @Transactional
  fun processEvent(domainEvent: DomainEvent) {
    val unmergedCrn = domainEvent.additionalInformation?.unmergedCrn!!
    val existingPerson = corePersonRecordAndDeliusClient
      .getPersonErrorIfNotFound(unmergedCrn)
      .let {
        probationProcessor.processProbationEvent(it)
      }

    val reactivatedCrn = domainEvent.additionalInformation.reactivatedCrn!!
    val reactivatedPerson = corePersonRecordAndDeliusClient
      .getPersonErrorIfNotFound(reactivatedCrn)
      .let {
        probationProcessor.processProbationEvent(it.doNotLinkOnCreate())
      }

    unmergeService.processUnmerge(reactivatedPerson, existingPerson)
  }
}
