package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personService: PersonService,
) {

  @Transactional
  fun processEvent(mergeDomainEvent: ProbationPersonMerged) {
    val toCrn = mergeDomainEvent.additionalInformation.targetCrn
    val fromCrn = mergeDomainEvent.additionalInformation.sourceCrn

    corePersonRecordAndDeliusClient.getProbationCase(toCrn).let {
      val from: PersonEntity? = personRepository.findByCrn(fromCrn)
      val person = Person.from(it)
      val to = personService.processPerson(person.doNotReclusterOnUpdate()) { personRepository.findByCrn(toCrn) }
      mergeService.processMerge(from, to)
    }
  }
}
