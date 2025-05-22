package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val createUpdateService: CreateUpdateService,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
) {

  fun processEvent(mergeDomainEvent: DomainEvent) {
    encodingService.getProbationCase(mergeDomainEvent.additionalInformation?.targetCrn!!) {
      it?.let {
        val from: PersonEntity? = personRepository.findByCrn(mergeDomainEvent.additionalInformation.sourceCrn!!)
        val to: PersonEntity = createUpdateService.processPerson(Person.from(it), shouldReclusterOnUpdate = false) { personRepository.findByCrn(mergeDomainEvent.additionalInformation.targetCrn) }
        mergeService.processMerge(from, to)
      }
    }
  }
}
