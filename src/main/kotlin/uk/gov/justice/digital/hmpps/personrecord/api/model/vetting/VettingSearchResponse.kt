package uk.gov.justice.digital.hmpps.personrecord.api.model.vetting

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
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

data class VettingSearchResponse(val data: List<VettingSearchData>)

data class VettingSearchData(
  val name: VettingName,
  val aliases: List<VettingAlias>,
  val addresses: List<VettingAddress>,
  val identifiers: List<VettingReference>,
  val sourceSystem: SourceSystemType,
  val status: VettingMatchStatus,
  @field:Schema(
    example = """[{"name":{"firstName":"John","middleNames":"John","lastName":"Doe"},"aliases":[],"addresses":[],"identifiers":[],"sourceSystem":"NOMIS","status":"HIGH_CONFIDENCE_MATCH"}]""",
  )
  var linkedRecords: List<VettingSearchData> = emptyList(),
)

data class VettingName(
  val firstName: String?,
  val middleNames: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
)

data class VettingAlias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
  val titleCode: TitleCode? = null,
  val dateOfBirth: LocalDate? = null,
  val sexCode: SexCode? = null,
) {
  companion object {
    fun from(pseudonymEntity: PseudonymEntity): VettingAlias = VettingAlias(
      titleCode = pseudonymEntity.titleCode,
      firstName = pseudonymEntity.firstName,
      middleNames = pseudonymEntity.middleNames,
      lastName = pseudonymEntity.lastName,
      dateOfBirth = pseudonymEntity.dateOfBirth,
      sexCode = pseudonymEntity.sexCode,
    )
  }
}

data class VettingAddress(
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
  val contacts: List<VettingContact> = emptyList(),
  var statusCode: AddressStatusCode? = null,
  var usages: List<VettingAddressUsage> = emptyList(),
  var recordType: AddressRecordType? = null,
  var deliusAddressId: Long? = null,
  var isVerified: Boolean? = null,
) {
  companion object {
    fun from(addressEntity: AddressEntity) = VettingAddress(
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
      recordType = addressEntity.recordType,
      statusCode = addressEntity.statusCode,
      deliusAddressId = addressEntity.deliusAddressId,
      isVerified = addressEntity.isVerified,
      usages = addressEntity.usages.map { VettingAddressUsage.from(it) },
      contacts = addressEntity.contacts.map { VettingContact.from(it) },
    )
  }
}

data class VettingAddressUsage(
  val addressUsageCode: AddressUsageCode,
  val isActive: Boolean,
) {
  companion object {
    fun from(addressUsageEntity: AddressUsageEntity) = VettingAddressUsage(
      addressUsageCode = addressUsageEntity.usageCode,
      isActive = addressUsageEntity.active,
    )
  }
}

data class VettingContact(
  val contactType: ContactType,
  val contactValue: String? = null,
  val extension: String? = null,
) {
  companion object {
    fun from(contactEntity: ContactEntity) = VettingContact(
      contactType = contactEntity.contactType,
      contactValue = contactEntity.contactValue,
      extension = contactEntity.extension,
    )
  }
}

data class VettingReference(
  val identifierType: IdentifierType,
  val identifierValue: String? = null,
  val comment: String? = null,
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity) = VettingReference(
      identifierType = referenceEntity.identifierType,
      identifierValue = referenceEntity.identifierValue,
      comment = referenceEntity.comment,
    )
  }
}

enum class VettingMatchStatus {
  HIGH_CONFIDENCE_MATCH,
  LOW_CONFIDENCE_MATCH,
  ;

  companion object {
    fun UUIDStatusType.toVettingStatus(): VettingMatchStatus = when (this) {
      ACTIVE -> HIGH_CONFIDENCE_MATCH
      else -> LOW_CONFIDENCE_MATCH
    }
  }
}
