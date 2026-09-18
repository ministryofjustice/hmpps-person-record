package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchStatus.Companion.toSearchStatus
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import java.time.LocalDate

data class VettingPersonSearchResponse(
  val data: List<VettingResult>,
)

data class VettingResult(
  val results: List<VettingSearchData>,
)

data class VettingSearchData(
  val name: SearchName,
  val aliases: List<CanonicalAlias>,
  val addresses: List<CanonicalAddress>,
  val identifiers: SearchIdentifiers,
  val sourceSystem: SourceSystemType,
  val status: SearchStatus,
) {
  companion object {
    fun from(personEntity: PersonEntity): VettingSearchData {
      val mainPseudonym = personEntity.getPrimaryName()
      return VettingSearchData(
        name = SearchName(
          firstName = mainPseudonym.firstName,
          middleNames = mainPseudonym.middleNames,
          lastName = mainPseudonym.lastName,
          dateOfBirth = mainPseudonym.dateOfBirth,
        ),
        aliases = CanonicalAlias.from(personEntity) ?: emptyList(),
        addresses = personEntity.addresses.map { CanonicalAddress.from(it) },
        identifiers = SearchIdentifiers.from(personEntity),
        sourceSystem = personEntity.sourceSystem,
        status = personEntity.personKey!!.status.toSearchStatus(),
      )
    }
  }
}

data class SearchIdentifiers(
  val crn: String? = null,
  val prisonNumber: String? = null,
  val defendantId: String? = null,
  val cid: String? = null,
  val pncs: List<String> = emptyList(),
  val cros: List<String> = emptyList(),
  val nationalInsuranceNumbers: List<String> = emptyList(),
  val driverLicenseNumbers: List<String> = emptyList(),
  val arrestSummonsNumbers: List<String> = emptyList(),
  val otherIdentifiers: List<String> = emptyList(),
) {
  companion object {
    fun from(personEntity: PersonEntity): SearchIdentifiers {
      val canonicalIdentifiers = CanonicalIdentifiers.from(listOf(personEntity))
      return SearchIdentifiers(
        crn = personEntity.crn,
        prisonNumber = personEntity.prisonNumber,
        defendantId = personEntity.defendantId,
        cid = personEntity.cId,
        pncs = canonicalIdentifiers.pncs,
        cros = canonicalIdentifiers.cros,
        nationalInsuranceNumbers = canonicalIdentifiers.nationalInsuranceNumbers,
        driverLicenseNumbers = canonicalIdentifiers.driverLicenseNumbers,
        arrestSummonsNumbers = canonicalIdentifiers.arrestSummonsNumbers,
        otherIdentifiers = canonicalIdentifiers.otherIdentifiers,
      )
    }
  }
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
