package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus.MOVED_PERMANENTLY
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import java.net.URI

@Component
class PrisonGetHandler(
  private val personRepository: PersonRepository,
  private val prisonReligionRepository: PrisonReligionRepository,
  private val prisonReferenceRepository: PrisonReferenceRepository,
) {

  fun get(prisonNumber: String): ResponseEntity<PrisonCanonicalRecord> {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
    return when {
      personEntity == null -> throw ResourceNotFoundException(prisonNumber)
      personEntity.hasMergedIntoAnotherPerson() -> respondWithRedirect(getMergedToPrisonNumber(personEntity))
      else -> {
        ResponseEntity.ok(
          PrisonCanonicalRecord.from(
            personEntity = personEntity,
            prisonReligionEntities = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber),
            prisonAlias = getPrisonSpecificReferences(personEntity, personEntity.pseudonyms),
          ),
        )
      }
    }
  }

  private fun getMergedToPrisonNumber(personEntity: PersonEntity): String = personRepository.findByIdOrNull(personEntity.mergedTo!!)!!.prisonNumber!!

  private fun PersonEntity.hasMergedIntoAnotherPerson() = !this.isNotMerged()

  private fun respondWithRedirect(targetPrisonNumber: String): ResponseEntity<PrisonCanonicalRecord> = ResponseEntity
    .status(MOVED_PERMANENTLY)
    .location(URI("/person/prison/$targetPrisonNumber"))
    .build()

  private fun getPrisonSpecificReferences(
    personEntity: PersonEntity,
    pseudonymEntities: List<PseudonymEntity>,
  ) = pseudonymEntities
    .associateWith { pseudonymEntity -> prisonReferenceRepository.findAllByPseudonym(pseudonymEntity) }
    .filter { it.key.nameType == NameType.ALIAS }
    .map { referenceEntitiesByPseudonymEntity ->
      PrisonAlias(
        alias = CanonicalAlias.from(referenceEntitiesByPseudonymEntity.key),
        identifiers = referenceEntitiesByPseudonymEntity.value.toCanonicalReferences(personEntity),
      )
    }

  private fun List<PrisonReferenceEntity>.toCanonicalReferences(personEntity: PersonEntity): CanonicalIdentifiers {
    val identifierValuesByType = this.groupBy { prisonReferenceEntity -> prisonReferenceEntity.identifierType }
      .mapValues { prisonReferenceEntitiesByType ->
        prisonReferenceEntitiesByType.value.mapNotNull { prisonReferenceEntity ->
          prisonReferenceEntity.identifierValue
        }
      }

    return CanonicalIdentifiers(
      crns = listOfNotNull(personEntity.crn),
      prisonNumbers = listOfNotNull(personEntity.prisonNumber),
      defendantIds = listOfNotNull(personEntity.defendantId),
      cids = listOfNotNull(personEntity.cId),
      cros = identifierValuesByType[CRO] ?: emptyList(),
      pncs = identifierValuesByType[PNC] ?: emptyList(),
      nationalInsuranceNumbers = identifierValuesByType[NATIONAL_INSURANCE_NUMBER] ?: emptyList(),
      arrestSummonsNumbers = identifierValuesByType[ARREST_SUMMONS_NUMBER] ?: emptyList(),
      driverLicenseNumbers = identifierValuesByType[DRIVER_LICENSE_NUMBER] ?: emptyList(),
    )
  }
}
