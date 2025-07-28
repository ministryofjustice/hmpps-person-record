package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class Address(
  val id: String?,
  val type: AddressType,
  val flat: String?,
  val premise: String?,
  val street: String?,
  val locality: String?,
  val townCode: String?,
  val postcode: String?,
  val countyCode: String?,
  val countryCode: String?,
  val noFixedAddress: String?,
  val startDate: String?,
  val endDate: String?,
  val comment: String?,
  val isPrimary: Boolean,
  val isMail: Boolean,
  val isActive: Boolean,
)

enum class AddressType {
  BUS,
  HOME,
  WORK,
}
