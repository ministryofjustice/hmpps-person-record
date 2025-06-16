package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val createUpdateService: CreateUpdateService,
  private val mergeService: MergeService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  fun processEvent(mergeDomainEvent: DomainEvent) {
    val toCrn = mergeDomainEvent.additionalInformation?.targetCrn!!
    val fromCrn = mergeDomainEvent.additionalInformation.sourceCrn!!

    corePersonRecordAndDeliusClient.getPerson(toCrn).let {
      val from: PersonEntity? = personRepository.findByCrn(fromCrn)
      val to: PersonEntity = createUpdateService.processPerson(
        it,
        shouldReclusterOnUpdate = false,
      ) { personRepository.findByCrn(toCrn) }
      mergeService.processMerge(from, to)
    }
  }
}
