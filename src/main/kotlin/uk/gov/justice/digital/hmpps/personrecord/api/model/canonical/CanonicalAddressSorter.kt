package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType.PREVIOUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM

object CanonicalAddressSorter {

  private val orderingStrategies: Map<SourceSystemType, Comparator<AddressEntity>> = mapOf(
    COMMON_PLATFORM to compareBy { it.recordType.recordTypeOrder() },
  )

  fun sort(addresses: List<AddressEntity>): List<AddressEntity> {
    val comparator = addresses.sourceSystem()?.let { orderingStrategies[it] } ?: return addresses
    return addresses.sortedWith(comparator)
  }

  private fun List<AddressEntity>.sourceSystem(): SourceSystemType? = this.firstNotNullOfOrNull { it.person?.sourceSystem }

  private fun AddressRecordType?.recordTypeOrder(): Int = when (this) {
    PRIMARY -> 0
    PREVIOUS -> 1
    null -> 2
  }
}


