package uk.gov.justice.digital.hmpps.personrecord.api.handler

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class SysconReligionInsertHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) {

  @Transactional
  fun handleInsert(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Map<String, String> {
    val (personEntity, currentPrisonReligion) = validateRequest(prisonNumber, prisonReligionRequest)
    val cprReligionIdByNomisId = saveReligionsMapped(prisonNumber, prisonReligionRequest).also {
      personEntity.religion = currentPrisonReligion.religionCode
      personService.processPerson(Person.from(personEntity)) { personEntity }
    }
    return cprReligionIdByNomisId
  }

  private fun validateRequest(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Pair<PersonEntity, PrisonReligion> {
    val currentPrisonReligion = prisonReligionRequest.getCurrentReligion() ?: throw IllegalArgumentException("Exactly one current prison religion must be sent for $prisonNumber")
    val nomisIdMap = HashMap<String, String>()
    prisonReligionRequest.religions.forEach {
      when (nomisIdMap.contains(it.nomisReligionId)) {
        true -> throw IllegalArgumentException("Duplicate nomis religion id '${it.nomisReligionId}' were detected for $prisonNumber")
        false -> nomisIdMap[it.nomisReligionId] = it.nomisReligionId
      }
    }

    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException(prisonNumber)
    if (prisonReligionRepository.findByPrisonNumber(prisonNumber).isNotEmpty()) throw ConflictException("Religion(s) already exists for $prisonNumber")
    return personEntity to currentPrisonReligion
  }

  private fun saveReligionsMapped(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Map<String, String> {
    val cprReligionIdByNomisId = HashMap<String, String>()
    prisonReligionRequest.religions.forEach { prisonReligion ->
      val religionEntity = PrisonReligionEntity.from(prisonNumber, prisonReligion)
      val prisonReligionEntity = prisonReligionRepository.save(religionEntity)
      cprReligionIdByNomisId[prisonReligion.nomisReligionId] = prisonReligionEntity.updateId.toString()
    }
    return cprReligionIdByNomisId
  }
}