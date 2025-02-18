package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import java.time.LocalDate

class CanonicalAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val postcode: String? = null,
) {
  companion object {

    fun from(addressEntity: AddressEntity): CanonicalAddress = CanonicalAddress(
      postcode = addressEntity.postcode,
      startDate = addressEntity.startDate,
      noFixedAbode = addressEntity.noFixedAbode,
    )
    fun fromAddressEntityList(addressEntity: List<AddressEntity>): List<CanonicalAddress> = addressEntity.map { from(it) }
  }
}
