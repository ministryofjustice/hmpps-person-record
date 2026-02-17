package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Service
class PersonExclusionService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun exclude(findPersonToBeExcluded: () -> PersonEntity?) {
    val personEntityToBeExcluded = findPersonToBeExcluded() ?: throw ResourceNotFoundException("Person not found")
    val personKeyEntity = personEntityToBeExcluded.personKey ?: throw ResourceNotFoundException("Person key not found")

    if (personEntityToBeExcluded.isPassive()) {
      return
    }

    personEntityToBeExcluded.markAsPassive()
    personRepository.save(personEntityToBeExcluded)

    if (personKeyEntity.personEntities.size <= 1) {
      personMatchService.deleteFromPersonMatch(personEntityToBeExcluded)
      return
    }

    personEntityToBeExcluded.removePersonKeyLink()
    val newPersonKeyEntity = PersonKeyEntity.new()
    personEntityToBeExcluded.assignToPersonKey(newPersonKeyEntity)

    personKeyRepository.save(newPersonKeyEntity)
    personMatchService.deleteFromPersonMatch(personEntityToBeExcluded)
    publisher.publishEvent(PersonKeyCreated(personEntityToBeExcluded, newPersonKeyEntity))
  }
}
