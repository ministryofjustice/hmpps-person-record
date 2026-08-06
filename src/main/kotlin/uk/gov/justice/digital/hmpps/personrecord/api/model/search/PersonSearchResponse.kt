package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchMatchStatus.Companion.toSearchStatus
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import java.time.LocalDate

data class PersonSearchResponse(val data: List<SearchData>)

data class SearchData(
  val name: SearchName,
  val aliases: List<CanonicalAlias>,
  val addresses: List<CanonicalAddress>,
  val identifiers: List<SearchIdentifier>,
  val sourceSystem: SourceSystemType,
  val status: SearchMatchStatus,
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
        identifiers = personEntity.references.map { SearchIdentifier.from(it) },
        sourceSystem = personEntity.sourceSystem,
        status = personEntity.personKey!!.status.toSearchStatus(),
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

data class SearchIdentifier(
  val type: IdentifierType,
  val value: String? = null,
  val comment: String? = null,
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity) = SearchIdentifier(
      type = referenceEntity.identifierType,
      value = referenceEntity.identifierValue,
      comment = referenceEntity.comment,
    )
  }
}

enum class SearchMatchStatus {
  HIGH_CONFIDENCE_MATCH,
  LOW_CONFIDENCE_MATCH,
  ;

  companion object {
    fun UUIDStatusType.toSearchStatus(): SearchMatchStatus = when (this) {
      ACTIVE -> HIGH_CONFIDENCE_MATCH
      else -> LOW_CONFIDENCE_MATCH
    }
  }
}
