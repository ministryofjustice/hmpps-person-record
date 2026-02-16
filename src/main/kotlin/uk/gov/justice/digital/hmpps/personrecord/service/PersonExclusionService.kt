package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository

@Service
class PersonExclusionService(
  private val personKeyRepository: PersonKeyRepository,
) {

  @Transactional
  fun exclude(findPersonToBeExcluded: () -> PersonEntity?) {
    val personEntityToBeExcluded = findPersonToBeExcluded() ?: throw ResourceNotFoundException("Person not found")
    val personKeyEntity = personEntityToBeExcluded.personKey ?: throw ResourceNotFoundException("Person key not found")

    if (personEntityToBeExcluded.isPassive()) {
      return
    }

    personEntityToBeExcluded.markAsPassive()

    if (personKeyEntity.personEntities.size <= 1) {
      return
    }

    personEntityToBeExcluded.removePersonKeyLink()
    val newPersonKeyEntity = PersonKeyEntity.new()

    personEntityToBeExcluded.assignToPersonKey(newPersonKeyEntity)

    personKeyRepository.save(newPersonKeyEntity)
  }
}
