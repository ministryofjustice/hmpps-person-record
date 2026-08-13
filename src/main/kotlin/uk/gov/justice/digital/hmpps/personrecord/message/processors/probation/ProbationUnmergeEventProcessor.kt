package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

@Component
class ProbationUnmergeEventProcessor(
  private val unmergeService: UnmergeService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val probationProcessor: ProbationProcessor,
) {

  @Transactional
  fun processEvent(domainEvent: ProbationPersonUnmerged) {
    val unmergedCrn = domainEvent.additionalInformation.unmergedCrn
    val existingPerson = corePersonRecordAndDeliusClient
      .getProbationCase(unmergedCrn)
      .let {
        probationProcessor.processProbationEvent(Person.from(it))
      }

    val reactivatedCrn = domainEvent.additionalInformation.reactivatedCrn
    val reactivatedPerson = corePersonRecordAndDeliusClient
      .getProbationCase(reactivatedCrn)
      .let {
        val person = Person.from(it)
        probationProcessor.processProbationEvent(person.doNotLinkOnCreate())
      }

    unmergeService.processUnmerge(reactivatedPerson, existingPerson)
  }
}
