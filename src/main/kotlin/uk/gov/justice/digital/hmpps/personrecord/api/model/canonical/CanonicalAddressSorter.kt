package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType.PREVIOUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM

object CanonicalAddressSorter {

  fun sort(addresses: List<AddressEntity>): List<AddressEntity> = when (addresses.sourceSystem()) {
    COMMON_PLATFORM -> addresses.sortedWith(compareBy { it.recordType.recordTypeOrder() })
    else -> addresses
  }

  private fun List<AddressEntity>.sourceSystem(): SourceSystemType? = this.firstNotNullOfOrNull { it.person?.sourceSystem }

  private fun AddressRecordType?.recordTypeOrder(): Int = when (this) {
    PRIMARY -> 0
    PREVIOUS -> 1
    null -> 2
  }
}
