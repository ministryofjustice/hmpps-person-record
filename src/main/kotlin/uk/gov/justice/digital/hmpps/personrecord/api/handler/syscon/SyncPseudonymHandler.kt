package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncPseudonymHandler.Companion.update
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

interface SysconSyncPseudonymHandler {
  fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias): SysconAliasMapping

  companion object {
    fun PseudonymEntity.update(prisonAlias: PrisonAlias, personEntity: PersonEntity): PseudonymEntity {
      nameType = if (prisonAlias.isPrimary) NameType.PRIMARY else NameType.ALIAS
      titleCode = prisonAlias.titleCode
      firstName = prisonAlias.firstName
      middleNames = prisonAlias.middleNames
      lastName = prisonAlias.lastName
      dateOfBirth = prisonAlias.dateOfBirth
      sexCode = prisonAlias.sexCode
      ethnicityCode = prisonAlias.ethnicity
      birthplace = prisonAlias.birthPlace
      birthCountryCode = prisonAlias.birthCountry
      createDateTime = prisonAlias.createDateTime!!
      createUserId = prisonAlias.createUserId!!
      modifyDateTime = prisonAlias.modifyDateTime
      modifyUserId = prisonAlias.modifyUserId
      person = personEntity
      return this
    }

    fun PrisonPseudonymEntity.update(prisonAlias: PrisonAlias): PrisonPseudonymEntity {
      suffix = prisonAlias.suffix
      nameType = prisonAlias.nameType
      return this
    }
  }
}

@Profile("prod | preprod")
@Component
@Transactional
class SysconSyncPseudonymHandlerProd : SysconSyncPseudonymHandler {
  override fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias) = SysconAliasMapping()
}

@Profile("!prod & !preprod")
@Component
@Transactional
class SysconSyncPseudonymHandlerImpl(
  private val personRepository: PersonRepository,
  private val pseudonymRepository: PseudonymRepository,
  private val prisonPseudonymRepository: PrisonPseudonymRepository,
  private val personService: PersonService,
) : SysconSyncPseudonymHandler {

  override fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias): SysconAliasMapping {
    val person = personRepository.findByPrisonNumber(prisonNumber)
    return when (person) {
      null -> createPerson(prisonNumber, prisonAlias)
      // TODO Once person creation has been implemented work on normal alias insertion.
      // TODO For now just return an empty mapping which the sync will ignore.
      else -> SysconAliasMapping()
    }
  }

  private fun validateCreateRequest(prisonNumber: String, prisonAlias: PrisonAlias) {
    if (!prisonAlias.isPrimary) {
      throw IllegalArgumentException("Cannot create a person record with a non-primary alias for $prisonNumber, ${prisonAlias.nomisOffenderId}")
    }
    if (prisonAlias.identifiers.isNotEmpty()) {
      val identifierIds = prisonAlias.identifiers.map { it.nomisIdentifierId }
      throw IllegalArgumentException("Identifiers must not be provided when creating a person record for $prisonNumber, ${prisonAlias.nomisOffenderId}: ${identifierIds.joinToString { "${it?.nomisOffenderId}-${it?.nomisSequence}" }}")
    }
  }

  private fun createPerson(prisonNumber: String, prisonAlias: PrisonAlias): SysconAliasMapping {
    validateCreateRequest(prisonNumber, prisonAlias)
    val personEntity = personService.processCreate(NOMIS) { personEntity ->
      personEntity.prisonNumber = prisonNumber

      val pseudonymEntity = pseudonymRepository.saveAndFlush(PseudonymEntity(nameType = NameType.PRIMARY).update(prisonAlias, personEntity))
      personEntity.pseudonyms.add(pseudonymEntity)

      prisonPseudonymRepository.save(PrisonPseudonymEntity(pseudonym = pseudonymEntity).update(prisonAlias))
      personEntity
    }

    return SysconAliasMapping(
      nomisOffenderId = prisonAlias.nomisOffenderId,
      cprAliasId = personEntity.getPrimaryName().updateId.toString(),
    )
  }
}
