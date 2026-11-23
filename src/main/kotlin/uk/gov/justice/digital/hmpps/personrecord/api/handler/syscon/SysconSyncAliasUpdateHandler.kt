package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.util.UUID

@Component
class SysconSyncAliasUpdateHandler(
  private val pseudonymRepository: PseudonymRepository,
  private val prisonPseudonymRepository: PrisonPseudonymRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleUpdate(prisonNumber: String, cprAliasId: String, prisonAlias: PrisonAlias) {
    val pseudonymEntity = pseudonymRepository.findByUpdateId(UUID.fromString(cprAliasId))
      ?: throw ResourceNotFoundException("Alias with $cprAliasId not found for person with $prisonNumber")

    personService.processUpdateAndPublish(pseudonymEntity.person!!) { personEntity ->
      prisonAlias.updateFields(pseudonymEntity)
      prisonAlias.updatePrisonFields(pseudonymEntity)
      if (pseudonymEntity.nameType == PRIMARY) personEntity.ethnicityCode = prisonAlias.ethnicity
    }
  }

  private fun PrisonAlias.updateFields(pseudonymEntity: PseudonymEntity) {
    pseudonymEntity.titleCode = titleCode
    pseudonymEntity.firstName = firstName
    pseudonymEntity.middleNames = middleNames
    pseudonymEntity.lastName = lastName
    pseudonymEntity.dateOfBirth = dateOfBirth
    pseudonymEntity.sexCode = sexCode
    pseudonymEntity.ethnicityCode = ethnicity
    pseudonymEntity.birthplace = birthPlace
    pseudonymEntity.birthCountryCode = birthCountry
    pseudonymEntity.modifyDateTime = modifyDateTime
    pseudonymEntity.modifyUserId = modifyUserId
  }

  private fun PrisonAlias.updatePrisonFields(pseudonymEntity: PseudonymEntity) {
    val prisonPseudonymEntity = prisonPseudonymRepository.findByIdOrNull(pseudonymEntity.id!!)
      ?: PrisonPseudonymEntity(pseudonym = pseudonymEntity)
    prisonPseudonymEntity.suffix = suffix
    prisonPseudonymEntity.nameType = nameType
    prisonPseudonymRepository.save(prisonPseudonymEntity)
  }
}
