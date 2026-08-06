package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import java.time.LocalDate
import java.time.ZonedDateTime

data class PersonSearchResponse(val data: List<SearchData>)

data class SearchData(
  val name: SearchName,
  val aliases: List<SearchAlias>,
  val addresses: List<SearchAddress>,
  val identifiers: List<SearchReference>,
  val sourceSystem: SourceSystemType,
  val status: SearchMatchStatus,
  @field:Schema(
    example = """[{"name":{"firstName":"John","middleNames":"John","lastName":"Doe"},"aliases":[],"addresses":[],"identifiers":[],"sourceSystem":"NOMIS","status":"HIGH_CONFIDENCE_MATCH"}]""",
  )
  var linkedRecords: List<SearchData> = emptyList(),
)

data class SearchName(
  val firstName: String?,
  val middleNames: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
)

data class SearchAlias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
  val titleCode: TitleCode? = null,
  val dateOfBirth: LocalDate? = null,
  val sexCode: SexCode? = null,
) {
  companion object {
    fun from(pseudonymEntity: PseudonymEntity): SearchAlias = SearchAlias(
      titleCode = pseudonymEntity.titleCode,
      firstName = pseudonymEntity.firstName,
      middleNames = pseudonymEntity.middleNames,
      lastName = pseudonymEntity.lastName,
      dateOfBirth = pseudonymEntity.dateOfBirth,
      sexCode = pseudonymEntity.sexCode,
    )
  }
}

data class SearchAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: ZonedDateTime? = null,
  val endDate: ZonedDateTime? = null,
  val postcode: String? = null,
  val fullAddress: String? = null,
  val subBuildingName: String? = null,
  val buildingName: String? = null,
  val buildingNumber: String? = null,
  val thoroughfareName: String? = null,
  val dependentLocality: String? = null,
  val postTown: String? = null,
  val county: String? = null,
  val countryCode: CountryCode? = null,
  val uprn: String? = null,
  val comment: String? = null,
  val contacts: List<SearchContact> = emptyList(),
  var statusCode: AddressStatusCode? = null,
  var usages: List<SearchAddressUsage> = emptyList(),
  var typeVerified: Boolean? = null,
) {
  companion object {
    fun from(addressEntity: AddressEntity) = SearchAddress(
      postcode = addressEntity.postcode,
      fullAddress = addressEntity.fullAddress,
      startDate = addressEntity.startDate,
      endDate = addressEntity.endDate,
      noFixedAbode = addressEntity.noFixedAbode,
      subBuildingName = addressEntity.subBuildingName,
      buildingName = addressEntity.buildingName,
      buildingNumber = addressEntity.buildingNumber,
      thoroughfareName = addressEntity.thoroughfareName,
      dependentLocality = addressEntity.dependentLocality,
      postTown = addressEntity.postTown,
      county = addressEntity.county,
      countryCode = addressEntity.countryCode,
      uprn = addressEntity.uprn,
      comment = addressEntity.comment,
      statusCode = addressEntity.statusCode,
      typeVerified = addressEntity.isVerified,
      usages = addressEntity.usages.map { SearchAddressUsage.from(it) },
      contacts = addressEntity.contacts.map { SearchContact.from(it) },
    )
  }
}

data class SearchAddressUsage(
  val addressUsageCode: AddressUsageCode,
  val isActive: Boolean,
) {
  companion object {
    fun from(addressUsageEntity: AddressUsageEntity) = SearchAddressUsage(
      addressUsageCode = addressUsageEntity.usageCode,
      isActive = addressUsageEntity.active,
    )
  }
}

data class SearchContact(
  val type: ContactType,
  val value: String? = null,
  val extension: String? = null,
) {
  companion object {
    fun from(contactEntity: ContactEntity) = SearchContact(
      type = contactEntity.contactType,
      value = contactEntity.contactValue,
      extension = contactEntity.extension,
    )
  }
}

data class SearchReference(
  val type: IdentifierType,
  val value: String? = null,
  val comment: String? = null,
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity) = SearchReference(
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
