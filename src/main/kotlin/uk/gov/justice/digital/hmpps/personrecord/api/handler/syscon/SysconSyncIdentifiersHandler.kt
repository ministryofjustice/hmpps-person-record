package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.util.UUID

@Component
class SysconSyncIdentifiersHandler(
  private val pseudonymRepository: PseudonymRepository,
  private val referenceRepository: ReferenceRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleReplace(prisonNumber: String, cprAliasId: String, request: PrisonIdentifiersRequest): SysconIdentifiersResponseBody {
    val pseudonymEntity = pseudonymRepository.findByUpdateId(UUID.fromString(cprAliasId))
      ?: throw ResourceNotFoundException("Alias with $cprAliasId not found for person with $prisonNumber")

    val mappings = personService.processUpdateAndPublish(pseudonymEntity.person!!) { personEntity ->
      val referenceEntities = referenceRepository.saveAllAndFlush(
        request.identifiers.map { it.findExistingElseNewReference(pseudonymEntity) },
      )
      val retainedIds = referenceEntities.map { it.updateId }.toSet()
      pseudonymEntity.references.removeIf { it.updateId !in retainedIds }
      personEntity.references.removeIf { it.pseudonym?.id == pseudonymEntity.id && it.updateId !in retainedIds }
      referenceEntities.forEach {
        if (it !in pseudonymEntity.references) pseudonymEntity.references.add(it)
        if (it !in personEntity.references) personEntity.references.add(it)
      }
      request.identifiers.zip(referenceEntities).map { (identifier, entity) -> identifier.toMapping(entity) }
    }
    return SysconIdentifiersResponseBody(prisonNumber, mappings)
  }

  private fun PrisonIdentifier.findExistingElseNewReference(pseudonymEntity: PseudonymEntity): ReferenceEntity {
    // only reuse an identifier already on this alias, otherwise create a new one
    val referenceEntity = cprIdentifierId.takeUnless { it.isNullOrBlank() }
      ?.let { id -> pseudonymEntity.references.firstOrNull { it.updateId.toString() == id } }
      ?: ReferenceEntity(identifierType = type, createDateTime = createDateTime, createUserId = createUserId)

    referenceEntity.identifierType = type
    referenceEntity.identifierValue = formattedValue()
    referenceEntity.comment = comment
    referenceEntity.isVerified = verified
    referenceEntity.person = pseudonymEntity.person
    referenceEntity.pseudonym = pseudonymEntity
    referenceEntity.modifyDateTime = modifyDateTime
    referenceEntity.modifyUserId = modifyUserId
    return referenceEntity
  }

  private fun PrisonIdentifier.toMapping(referenceEntity: ReferenceEntity) = SysconIdentifierMapping(
    nomisIdentifierId = NomisIdentifierId(
      nomisOffenderId = nomisIdentifierId.nomisOffenderId,
      nomisSequence = nomisIdentifierId.nomisSequence,
    ),
    cprIdentifierId = referenceEntity.updateId.toString(),
  )
}
