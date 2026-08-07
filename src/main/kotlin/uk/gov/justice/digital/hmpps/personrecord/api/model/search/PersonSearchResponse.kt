package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchStatus.Companion.toSearchStatus
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.OTHR
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import java.time.LocalDate

data class PersonSearchResponse(val data: List<SearchData>)

data class SearchData(
  val name: SearchName,
  val aliases: List<CanonicalAlias>,
  val addresses: List<CanonicalAddress>,
  val identifiers: CanonicalIdentifiers,
  val sourceSystem: SourceSystemType,
  val status: SearchStatus,
  @field:Schema(example = """[{"name":{"firstName":"","middleNames":"","lastName":"","dateOfBirth":""},"aliases":[],"addresses":[],"identifiers":[],"sourceSystem":"","status":""}]""")
  var linkedRecords: List<SearchData> = emptyList(),
) {
  companion object {
    fun from(personEntity: PersonEntity): SearchData {
      val mainPseudonym = personEntity.getPrimaryName()
      return SearchData(
        name = SearchName(
          firstName = mainPseudonym.firstName,
          middleNames = mainPseudonym.middleNames,
          lastName = mainPseudonym.lastName,
          dateOfBirth = mainPseudonym.dateOfBirth,
        ),
        aliases = CanonicalAlias.from(personEntity) ?: emptyList(),
        addresses = personEntity.addresses.map { CanonicalAddress.from(it) },
        identifiers = CanonicalIdentifiers.forSinglePerson(personEntity),
        sourceSystem = personEntity.sourceSystem,
        status = personEntity.personKey!!.status.toSearchStatus(),
      )
    }
  }
}

fun CanonicalIdentifiers.Companion.forSinglePerson(personEntity: PersonEntity): CanonicalIdentifiers {
  val referenceEntities = personEntity.references
    .groupBy { it.identifierType }
    .mapValues { entry -> entry.value.mapNotNull { it.identifierValue } }
  return CanonicalIdentifiers(
    crns = personEntity.crn?.let { listOf(it) } ?: emptyList(),
    prisonNumbers = personEntity.prisonNumber?.let { listOf(it) } ?: emptyList(),
    defendantIds = personEntity.defendantId?.let { listOf(it) } ?: emptyList(),
    cids = personEntity.cId?.let { listOf(it) } ?: emptyList(),
    cros = referenceEntities.getOrDefault(CRO, emptyList()),
    pncs = referenceEntities.getOrDefault(PNC, emptyList()),
    nationalInsuranceNumbers = referenceEntities.getOrDefault(NATIONAL_INSURANCE_NUMBER, emptyList()),
    arrestSummonsNumbers = referenceEntities.getOrDefault(ARREST_SUMMONS_NUMBER, emptyList()),
    driverLicenseNumbers = referenceEntities.getOrDefault(DRIVER_LICENSE_NUMBER, emptyList()),
    otherIdentifiers = referenceEntities.getOrDefault(OTHR, emptyList()),
  )
}

data class SearchName(
  val firstName: String?,
  val middleNames: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
)

enum class SearchStatus {
  TRUSTED,
  NOT_TRUSTED,
  ;

  companion object {
    fun UUIDStatusType.toSearchStatus(): SearchStatus = when (this) {
      ACTIVE -> TRUSTED
      else -> NOT_TRUSTED
    }
  }
}
