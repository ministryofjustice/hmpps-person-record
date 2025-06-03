package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class ProbationUnmergeEventProcessor(
  private val unmergeService: UnmergeService,
  private val encodingService: EncodingService,
  private val personService: PersonService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    val existingPerson = getExistingPerson(domainEvent.additionalInformation?.unmergedCrn!!)
    val reactivatedPerson = getReactivatedPerson(domainEvent.additionalInformation.reactivatedCrn!!)
    unmergeService.processUnmerge(reactivatedPerson, existingPerson)
  }

  private fun getReactivatedPerson(crn: String): PersonEntity = encodingService.getProbationCase(crn) {
    personService.processPerson(Person.from(it!!)) { personProcessor ->
      personProcessor
        .find { searchProcessor -> searchProcessor.findByCrn(it.identifiers.crn!!) }
        .exists(
          no = { createProcessor, ctx -> createProcessor.createPersonEntity(ctx) },
          yes = { updateProcessor, ctx -> updateProcessor.updatePersonEntity(ctx) },
        )
        .recordEventLog()
        .get()
    }
  } as PersonEntity

  private fun getExistingPerson(crn: String): PersonEntity = encodingService.getProbationCase(crn) {
    personService.processPerson(Person.from(it!!)) { personProcessor ->
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
  } as PersonEntity
}
