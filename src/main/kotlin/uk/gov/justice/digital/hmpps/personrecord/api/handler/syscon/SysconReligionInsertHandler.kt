package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService.PersonLookup.ByPrisonNumber

@Component
class SysconReligionInsertHandler(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleInsert(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest): Map<String, String> = personService.processUpdate(ByPrisonNumber(prisonNumber)) { personEntity ->
    val currentPrisonReligion = validateRequest(personEntity, prisonReligionRequest)
    prisonReligionRepository.deleteAllByPrisonNumber(prisonNumber).also { prisonReligionRepository.flush() }
    saveReligionsMapped(prisonNumber, prisonReligionRequest).also {
      personEntity.religion = currentPrisonReligion.religionCode
    }
  }

  private fun validateRequest(personEntity: PersonEntity, prisonReligionRequest: PrisonReligionRequest): PrisonReligionHistory {
    val currentPrisonReligion = prisonReligionRequest.getCurrentReligion() ?: throw IllegalArgumentException("Exactly one current prison religion must be sent for ${personEntity.prisonNumber}")
    val nomisIdSet = hashSetOf<String>()
    prisonReligionRequest.religions.forEach {
      when (nomisIdSet.contains(it.nomisReligionId)) {
        true -> throw IllegalArgumentException("Duplicate nomis religion id '${it.nomisReligionId}' were detected for ${personEntity.prisonNumber}")
        false -> nomisIdSet.add(it.nomisReligionId)
      }
    }
    return currentPrisonReligion
  }

  private fun saveReligionsMapped(prisonNumber: String, prisonReligionRequest: PrisonReligionRequest) = prisonReligionRequest.religions.associate { prisonReligion ->
    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligion))
    prisonReligion.nomisReligionId to prisonReligionEntity.updateId.toString()
  }
}
