package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
  private val personService: PersonService,
) {

  fun processEvent(mergeDomainEvent: DomainEvent) {
    encodingService.getProbationCase(mergeDomainEvent.additionalInformation?.targetCrn!!) {
      it?.let {
        val from: PersonEntity? = personRepository.findByCrn(mergeDomainEvent.additionalInformation.sourceCrn!!)
        val to: PersonEntity = personService.processPerson(Person.from(it)) { personProcessor ->
          personProcessor
            .find { searchProcessor -> searchProcessor.findByCrn(it.identifiers.crn!!) }
            .exists(
              no = { createProcessor, ctx -> createProcessor.createPersonEntity(ctx) },
              yes = { updateProcessor, ctx -> updateProcessor.updatePersonEntity(ctx) },
            )
            .hasClusterLink(
              no = { clusterProcessor, ctx -> clusterProcessor.linkRecordToPersonKey(ctx) },
            )
            .recordEventLog()
            .get()
        }
        mergeService.processMerge(from, to)
      }
    }
  }
}
