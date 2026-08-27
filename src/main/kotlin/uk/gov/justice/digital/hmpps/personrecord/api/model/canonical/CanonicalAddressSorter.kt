package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM

object CanonicalAddressSorter {

  fun sort(addresses: List<AddressEntity>): List<AddressEntity> = when (addresses.sourceSystem()) {
    COMMON_PLATFORM -> addresses.sortedWith(compareBy { it.statusCode.statusCodeOrder() })
    else -> addresses
  }

  private fun List<AddressEntity>.sourceSystem(): SourceSystemType? = this.firstNotNullOfOrNull { it.person?.sourceSystem }

  private fun AddressStatusCode?.statusCodeOrder(): Int = when (this) {
    AddressStatusCode.M -> 0
    else -> 1
  }
}
