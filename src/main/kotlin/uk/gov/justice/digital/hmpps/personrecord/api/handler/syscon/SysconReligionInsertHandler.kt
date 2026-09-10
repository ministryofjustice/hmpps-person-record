package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

interface SysconReligionInsertHandler {
  fun handleInsert(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Map<String, String>
}

@Component
@Profile("preprod | prod")
class SysconReligionInsertHandlerProd(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) : SysconReligionInsertHandler {

  @Transactional
  override fun handleInsert(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Map<String, String> {
    val (personEntity, currentPrisonReligion) = validateRequest(prisonNumber, prisonReligionRequest)
    prisonReligionRepository.deleteAllByPrisonNumber(prisonNumber).also { prisonReligionRepository.flush() }
    val cprReligionIdByNomisId = saveReligionsMapped(prisonNumber, prisonReligionRequest).also {
      personEntity.religion = currentPrisonReligion.religionCode
      personService.processPerson(Person.from(personEntity)) { personEntity }
    }
    return cprReligionIdByNomisId
  }

  private fun validateRequest(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Pair<PersonEntity, PrisonReligionHistory> {
    val currentPrisonReligion = prisonReligionRequest.getCurrentReligion() ?: throw IllegalArgumentException("Exactly one current prison religion must be sent for $prisonNumber")
    val nomisIdSet = hashSetOf<String>()
    prisonReligionRequest.religions.forEach {
      when (nomisIdSet.contains(it.nomisReligionId)) {
        true -> throw IllegalArgumentException("Duplicate nomis religion id '${it.nomisReligionId}' were detected for $prisonNumber")
        false -> nomisIdSet.add(it.nomisReligionId)
      }
    }

    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException(prisonNumber)
    return personEntity to currentPrisonReligion
  }

  private fun saveReligionsMapped(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest) = prisonReligionRequest.religions.associate { prisonReligion ->
    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligion))
    prisonReligion.nomisReligionId to prisonReligionEntity.updateId.toString()
  }
}

@Component
@Profile("!preprod & !prod")
class SysconReligionInsertHandlerDev(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) : SysconReligionInsertHandler {

  @Transactional
  override fun handleInsert(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Map<String, String> {
    val (personEntity, currentPrisonReligion) = validateRequest(prisonNumber, prisonReligionRequest)
    prisonReligionRepository.deleteAllByPrisonNumber(prisonNumber).also { prisonReligionRepository.flush() }
    val cprReligionIdByNomisId = saveReligionsMapped(prisonNumber, prisonReligionRequest).also {
      personEntity.religion = currentPrisonReligion.religionCode
      personService.processPerson(Person.from(personEntity), setOf(PseudonymEntity::class)) { personEntity }
    }
    return cprReligionIdByNomisId
  }

  private fun validateRequest(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Pair<PersonEntity, PrisonReligionHistory> {
    val currentPrisonReligion = prisonReligionRequest.getCurrentReligion() ?: throw IllegalArgumentException("Exactly one current prison religion must be sent for $prisonNumber")
    val nomisIdSet = hashSetOf<String>()
    prisonReligionRequest.religions.forEach {
      when (nomisIdSet.contains(it.nomisReligionId)) {
        true -> throw IllegalArgumentException("Duplicate nomis religion id '${it.nomisReligionId}' were detected for $prisonNumber")
        false -> nomisIdSet.add(it.nomisReligionId)
      }
    }

    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException(prisonNumber)
    return personEntity to currentPrisonReligion
  }

  private fun saveReligionsMapped(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest) = prisonReligionRequest.religions.associate { prisonReligion ->
    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligion))
    prisonReligion.nomisReligionId to prisonReligionEntity.updateId.toString()
  }
}
