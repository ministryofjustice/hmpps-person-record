package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
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
    val personEntity = getPersonEntityIfApplicableForExclusion(prisonerNumber)

    // remove link to person key
    personEntity.removePersonKeyLink()

    // create a new person key
    val newPersonKeyEntity = PersonKeyEntity.new()

    // attach person to new person key
    personEntity.assignToPersonKey(newPersonKeyEntity)

    // actually update
    personKeyRepository.save(newPersonKeyEntity)

    // delete from person match

    // recluster?!?


  }

  private fun getPersonEntityIfApplicableForExclusion(prisonerNumber: String): PersonEntity {
    val personEntity = personRepository.findByPrisonNumber(prisonerNumber) ?: throw ResourceNotFoundException("Person with prisoner $prisonerNumber not found")
    val personKeyEntityId = personEntity.personKey?.id ?: throw ResourceNotFoundException("Person key was not retrieved with person for prisoner $prisonerNumber")
    val personKeyEntity = personKeyRepository.findById(personKeyEntityId).orElseThrow { ResourceNotFoundException("Person key with id $personKeyEntityId not found") }
    validateIsApplicableForExclusion(personKeyEntity)
    return personEntity
  }

  private fun validateIsApplicableForExclusion(personKeyEntity: PersonKeyEntity) {
    if (personKeyEntity.personEntities.size <= 1) {
      // skip?
    }
  }
}