package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import java.time.LocalDate
import kotlin.reflect.full.memberProperties
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Address as CommonPlatformAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Address as LibraAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress as OffenderAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Address as PrisonerAddress

data class Address(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val postcode: String? = null,
  val fullAddress: String? = null,
  val subBuildingName: String? = null,
  val buildingName: String? = null,
  val buildingNumber: String? = null,
  val thoroughfareName: String? = null,
  val dependentLocality: String? = null,
  val postTown: String? = null,
  val county: String? = null,
  val countryCode: String? = null,
  val uprn: String? = null,
  val comment: String? = null,
  val contacts: List<Contact> = emptyList(),
  var isPrimary: Boolean? = null,
  var isMail: Boolean? = null,
  var usages: List<AddressUsage> = emptyList(),
  var recordType: AddressRecordType? = null,
) {

  fun allPropertiesOrNull(): Address? = this.takeIf { it.hasAnyMeaningfulProperty() }

  private fun hasAnyMeaningfulProperty(): Boolean = this::class.memberProperties.any { prop ->
    when (val value = prop.call(this)) {
      null -> false
      is Collection<*> -> value.isNotEmpty()
      else -> true
    }
  }

  companion object {
    fun from(address: PrisonerAddress): Address? = Address(
      postcode = address.postcode.nullIfBlank(),
      fullAddress = address.fullAddress.nullIfBlank(),
      startDate = address.startDate,
      noFixedAbode = address.noFixedAbode,
    ).allPropertiesOrNull()

    fun from(address: OffenderAddress): Address? = Address(
      noFixedAbode = address.noFixedAbode,
      startDate = address.startDate,
      endDate = address.endDate,
      postcode = address.postcode.nullIfBlank(),
      fullAddress = address.fullAddress.nullIfBlank(),
      buildingName = address.buildingName.nullIfBlank(),
      buildingNumber = address.addressNumber.nullIfBlank(),
      thoroughfareName = address.streetName.nullIfBlank(),
      dependentLocality = address.district.nullIfBlank(),
      postTown = address.townCity.nullIfBlank(),
      county = address.county.nullIfBlank(),
      uprn = address.uprn.nullIfBlank(),
      comment = address.notes.nullIfBlank(),
      contacts = address.telephoneNumber?.let { listOf(Contact(ContactType.HOME, it)) } ?: emptyList(),
    ).allPropertiesOrNull()

    fun from(address: CommonPlatformAddress?): Address? = Address(
      postcode = address?.postcode.nullIfBlank(),
      buildingName = address?.address1.nullIfBlank(),
      buildingNumber = address?.address2.nullIfBlank(),
      thoroughfareName = address?.address3.nullIfBlank(),
      dependentLocality = address?.address4.nullIfBlank(),
      postTown = address?.address5.nullIfBlank(),
    ).allPropertiesOrNull()

    fun from(address: LibraAddress?): Address? = Address(
      postcode = address?.postcode.nullIfBlank(),
      buildingName = address?.buildingName.nullIfBlank(),
      buildingNumber = address?.buildingNumber.nullIfBlank(),
      thoroughfareName = address?.thoroughfareName.nullIfBlank(),
      dependentLocality = address?.dependentLocality.nullIfBlank(),
      postTown = address?.postTown.nullIfBlank(),
    ).allPropertiesOrNull()

    fun fromPrisonerAddressList(addresses: List<PrisonerAddress>): List<Address> = addresses.mapNotNull { from(it) }

    fun fromOffenderAddressList(addresses: List<OffenderAddress>): List<Address> = addresses.mapNotNull { from(it) }

    fun from(addressEntity: AddressEntity): Address = Address(
      postcode = addressEntity.postcode,
      fullAddress = addressEntity.fullAddress,
      startDate = addressEntity.startDate,
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
    )
  }

  fun setToPrimary(): Address {
    this.recordType = AddressRecordType.PRIMARY
    return this
  }

  fun setToPrevious(): Address {
    this.recordType = AddressRecordType.PREVIOUS
    return this
  }
}
