package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class CanonicalAddress(
  val noFixedAbode: String? = null,
  val startDate: String? = null,
  val endDate: String? = null,
  val postcode: String? = null,
) {
  companion object {

    fun from(addressEntity: AddressEntity): CanonicalAddress = CanonicalAddress(
      postcode = addressEntity.postcode,
      startDate = addressEntity.startDate?.toString() ?: "",
      endDate = addressEntity.endDate?.toString() ?: "",
      noFixedAbode = addressEntity.noFixedAbode.toString(),
    )
    fun fromAddressEntityList(addressEntity: List<AddressEntity>): List<CanonicalAddress> = addressEntity.map { from(it) }
  }
}
