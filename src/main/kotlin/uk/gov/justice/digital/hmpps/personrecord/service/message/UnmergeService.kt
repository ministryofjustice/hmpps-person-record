package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class UnmergeService(
  private val personKeyService: PersonKeyService,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
  private val overrideService: OverrideService,
) {

  fun processUnmerge(reactivated: Person, existing: Person) {
    val existingPersonEntity = personRepository.findByCrn(existing.crn!!)!!
      .also { personService.update(existing, it) }

    val reactivatedPersonEntity = personRepository.findByCrn(reactivated.crn!!)!!
      .also { personService.update(reactivated, it) }

    unmerge(reactivatedPersonEntity, existingPersonEntity)
  }

  private fun unmerge(reactivated: PersonEntity, existing: PersonEntity) {
    reactivated.removeMergedLink()
    overrideService.systemExclude(reactivated, existing)

    personKeyService.linkRecordToPersonKey(reactivated)
    publisher.publishEvent(PersonUnmerged(reactivated, existing))
  }
}
