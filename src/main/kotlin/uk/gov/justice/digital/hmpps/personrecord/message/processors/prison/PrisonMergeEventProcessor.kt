package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

@Component
class PrisonMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
  private val createUpdateService: CreateUpdateService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    encodingService.getPrisonerDetails(domainEvent.additionalInformation?.prisonNumber!!) {
      it?.let {
        val from: PersonEntity? = personRepository.findByPrisonNumber(domainEvent.additionalInformation.sourcePrisonNumber!!)
        val to: PersonEntity = createUpdateService.processPerson(Person.from(it), shouldReclusterOnUpdate = false) { personRepository.findByPrisonNumber(domainEvent.additionalInformation.prisonNumber) }
        mergeService.processMerge(from, to)
      }
    }
  }
}
