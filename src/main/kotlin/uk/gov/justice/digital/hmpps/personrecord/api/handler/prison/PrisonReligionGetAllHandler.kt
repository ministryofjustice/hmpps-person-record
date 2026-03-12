package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository

@Component
class PrisonReligionGetAllHandler(
  private val personRepository: PersonRepository,
  private val prisonReligionRepository: PrisonReligionRepository,
) {

  fun get(prisonNumber: String): Result = getPersonEntityByPrisonNumber(personRepository.findByPrisonNumber(prisonNumber))?.let { personEntity ->
    when (hasMergedIntoAnotherPerson(personEntity, prisonNumber)) {
      true -> Result(targetPrisonNumber = personEntity.prisonNumber)
      false -> Result(PrisonCanonicalRecord.from(personEntity, getPrisonReligionsSorted(prisonNumber)))
    }
  } ?: throw ResourceNotFoundException(prisonNumber)

  private fun getPersonEntityByPrisonNumber(person: PersonEntity?): PersonEntity? = person?.mergedTo?.let {
    getPersonEntityByPrisonNumber(personRepository.findByIdOrNull(id = it))
  } ?: person

  private fun hasMergedIntoAnotherPerson(personEntity: PersonEntity, prisonNumber: String) = personEntity.prisonNumber != prisonNumber

  private fun getPrisonReligionsSorted(prisonNumber: String): List<PrisonReligionEntity> = prisonReligionRepository
    .findByPrisonNumber(prisonNumber)
    .sortedBy { it.id }

  data class Result(
    val prisonCanonicalRecord: PrisonCanonicalRecord? = null,
    val targetPrisonNumber: String? = null,
  )
}
