package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

@Component
class ProbationUnmergeEventProcessor(
  private val unmergeService: UnmergeService,
) {

  @Transactional
  fun processEvent(domainEvent: ProbationPersonUnmerged) {
    val unmergedCrn = domainEvent.additionalInformation.unmergedCrn
    val reactivatedCrn = domainEvent.additionalInformation.reactivatedCrn

    unmergeService.processUnmerge(reactivatedCrn, unmergedCrn)
  }
}
