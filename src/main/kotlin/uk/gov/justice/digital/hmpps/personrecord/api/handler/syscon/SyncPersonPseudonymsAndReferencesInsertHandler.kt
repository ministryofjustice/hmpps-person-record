package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SyncInsertHandlerHelper.addIfAbsent
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncPseudonymHandler.Companion.update
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.util.UUID

@Component
class SyncPersonPseudonymsAndReferencesInsertHandler(
  private val personRepository: PersonRepository,
  private val pseudonymRepository: PseudonymRepository,
  private val prisonPseudonymRepository: PrisonPseudonymRepository,
  private val referenceRepository: ReferenceRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleInsertForMigration(
    prisonNumber: String,
    prisonPseudonymsAndReferencesRequest: PrisonAliasesAndIdentifiersRequest,
  ): SysconAliasesAndIdentifiersResponseBody {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    return personService.processUpdate(personEntity) { personEntity, _ ->
      insertPseudonymsAndReferences(personEntity, prisonPseudonymsAndReferencesRequest)
    }
  }

  @Transactional
  fun insertPseudonymsAndReferences(
    personEntity: PersonEntity,
    prisonPseudonymsAndReferencesRequest: PrisonAliasesAndIdentifiersRequest,
  ): SysconAliasesAndIdentifiersResponseBody {
    validateRequest(personEntity.prisonNumber!!, prisonPseudonymsAndReferencesRequest.aliases)
    val mappings = handlePseudonymsAndReferencesInsert(prisonPseudonymsAndReferencesRequest.aliases, personEntity)
    return SysconAliasesAndIdentifiersResponseBody(personEntity.prisonNumber!!, mappings)
  }

  private fun validateRequest(prisonNumber: String, pseudonyms: List<PrisonAlias>) {
    val pseudonymNomisDuplicateIds = pseudonyms.groupingBy { it.nomisOffenderId }.eachCount().filter { it.value > 1 }
    if (pseudonymNomisDuplicateIds.isNotEmpty()) {
      throw IllegalArgumentException("Duplicate nomis pseudonym offender ids were detected for $prisonNumber: ${pseudonymNomisDuplicateIds.keys.joinToString()}")
    }
    val primaryPseudonymsIds = pseudonyms.filter { it.isPrimary }.map { it.nomisOffenderId }
    if (primaryPseudonymsIds.size != 1) {
      throw IllegalArgumentException("There must be exactly one primary pseudonym for $prisonNumber: ${primaryPseudonymsIds.joinToString()}")
    }
    pseudonyms.forEach { pseudonym ->
      val incorrectReferenceIds = pseudonym.identifiers
        .map { it.nomisIdentifierId!!.nomisOffenderId }
        .filter { it != pseudonym.nomisOffenderId }
      if (incorrectReferenceIds.isNotEmpty()) {
        throw IllegalArgumentException("Incorrect nomis reference ids were detected for $prisonNumber, ${pseudonym.nomisOffenderId}: ${incorrectReferenceIds.joinToString()}")
      }
      val referenceDuplicateIds = pseudonym.identifiers.groupingBy { it.nomisIdentifierId }.eachCount().filter { it.value > 1 }
      if (referenceDuplicateIds.isNotEmpty()) {
        throw IllegalArgumentException("Duplicate nomis reference ids were detected for $prisonNumber, ${pseudonym.nomisOffenderId}: ${referenceDuplicateIds.keys.joinToString { "$it.nomisOffenderId-$it.nomisSequence" }}")
      }
    }
  }

  private fun handlePseudonymsAndReferencesInsert(
    pseudonyms: List<PrisonAlias>,
    personEntity: PersonEntity,
  ): List<SysconAliasMapping> {
    val mappings = pseudonyms.map { pseudonym ->
      val pseudonymEntity = pseudonymRepository.saveAndFlush(pseudonym.findExistingElseNewPseudonymAndUpdateFields(personEntity))
      personEntity.pseudonyms.addIfAbsent(pseudonymEntity)

      prisonPseudonymRepository.save(pseudonym.findExistingElseNewPrisonPseudonymAndUpdateFields(pseudonymEntity))

      val referenceEntities = referenceRepository.saveAllAndFlush(
        pseudonym.identifiers.map { it.findExistingElseNewReferenceAndUpdateFields(personEntity, pseudonymEntity) },
      )
      pseudonymEntity.references.addIfAbsent(referenceEntities)
      personEntity.references.addIfAbsent(referenceEntities)
      val identifierMappings = pseudonym.identifiers.zip(referenceEntities).map { it.toMapping() }

      SysconAliasMapping(
        nomisOffenderId = pseudonym.nomisOffenderId,
        cprAliasId = pseudonymEntity.updateId.toString(),
        identifierMappings = identifierMappings,
      )
    }
    // Hibernate cascade deletes. Therefore, we can only remove entities at the end
    personEntity.removeAllPseudonymsBut(mappings.map { it.cprAliasId!! })
    personEntity.removeAllReferencesBut(mappings.flatMap { it.identifierMappings }.map { it.cprIdentifierId })
    return mappings
  }

  private fun PersonEntity.removeAllPseudonymsBut(toRetain: List<String>) = pseudonyms.removeIf { it.updateId.toString() !in toRetain }

  private fun PersonEntity.removeAllReferencesBut(toRetain: List<String>) {
    pseudonyms.forEach { it.removeAllReferencesBut(toRetain) }
    references.removeIf { it.updateId.toString() !in toRetain }
  }

  private fun PseudonymEntity.removeAllReferencesBut(toRetain: List<String>) = references.removeIf { it.updateId.toString() !in toRetain }

  private fun PrisonAlias.findExistingElseNewPseudonymAndUpdateFields(personEntity: PersonEntity): PseudonymEntity {
    val nameType = if (isPrimary) NameType.PRIMARY else NameType.ALIAS
    // if no id is passed in, or if the id no longer exists then we create a new one
    val pseudonymEntity = cprAliasId.takeUnless { it.isNullOrBlank() }?.let {
      pseudonymRepository.findByUpdateId(UUID.fromString(it))
    } ?: PseudonymEntity(nameType = nameType)
    return pseudonymEntity.update(this, personEntity)
  }

  private fun PrisonAlias.findExistingElseNewPrisonPseudonymAndUpdateFields(pseudonymEntity: PseudonymEntity): PrisonPseudonymEntity {
    val prisonPseudonymEntity = prisonPseudonymRepository.findByIdOrNull(pseudonymEntity.id!!)
      ?: PrisonPseudonymEntity(pseudonym = pseudonymEntity)
    return prisonPseudonymEntity.update(this)
  }

  private fun PrisonIdentifier.findExistingElseNewReferenceAndUpdateFields(personEntity: PersonEntity, pseudonymEntity: PseudonymEntity): ReferenceEntity {
    // if no id is passed in, or if the id no longer exists then we create a new one
    val referenceEntity = cprIdentifierId.takeUnless { it.isNullOrBlank() }?.let {
      referenceRepository.findByUpdateId(UUID.fromString(it))
    } ?: ReferenceEntity(identifierType = type)

    referenceEntity.identifierType = type
    referenceEntity.identifierValue = formattedValue()
    referenceEntity.comment = comment
    referenceEntity.isVerified = verified
    referenceEntity.person = personEntity
    referenceEntity.pseudonym = pseudonymEntity
    referenceEntity.modifyDateTime = modifyDateTime
    referenceEntity.modifyUserId = modifyUserId
    referenceEntity.createDateTime = createDateTime!!
    referenceEntity.createUserId = createUserId!!
    return referenceEntity
  }

  private fun Pair<PrisonIdentifier, ReferenceEntity>.toMapping() = SysconIdentifierMapping(
    nomisIdentifierId = NomisIdentifierId(
      nomisOffenderId = first.nomisIdentifierId!!.nomisOffenderId,
      nomisSequence = first.nomisIdentifierId!!.nomisSequence,
    ),
    cprIdentifierId = second.updateId.toString(),
  )
}
