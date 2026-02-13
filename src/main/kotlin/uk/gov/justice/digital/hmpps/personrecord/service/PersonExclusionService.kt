package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Service
class PersonExclusionService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService
) {

  @Transactional
  fun exclude(prisonerNumber: String) {
    val personEntityToBeExcluded = personRepository.findByPrisonNumber(prisonerNumber) ?: throw ResourceNotFoundException("Person with prisoner $prisonerNumber not found")
    val personKeyEntity = personEntityToBeExcluded.personKey ?: throw ResourceNotFoundException("Person with prisoner $prisonerNumber not found")

    // do marker stuff

    if (personKeyEntity.personEntities.size <= 1) {
      return
    }

    // remove link to person key
    personEntityToBeExcluded.removePersonKeyLink()

    // create a new person key
    val newPersonKeyEntity = PersonKeyEntity.new()

    // attach person to new person key
    personEntityToBeExcluded.assignToPersonKey(newPersonKeyEntity)

    // actually update
    personKeyRepository.save(newPersonKeyEntity)

    // delete from person match
    personMatchService.deleteFromPersonMatch(personEntityToBeExcluded)

    // recluster?!?


  }
}