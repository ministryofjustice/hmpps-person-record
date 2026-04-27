package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity

data class CanonicalAddressUsage(
  @JsonUnwrapped
  val usageCode: CanonicalAddressUsageCode,
  @Schema(description = "Address usage active flag", example = "true")
  val isActive: Boolean,
) {

  companion object {
    fun from(addressUsageEntity: AddressUsageEntity): CanonicalAddressUsage = CanonicalAddressUsage(
      usageCode = CanonicalAddressUsageCode.from(addressUsageEntity.usageCode),
      isActive = addressUsageEntity.active,
    )

    fun fromAddressUsageEntityList(addressUsageEntities: List<AddressUsageEntity>): List<CanonicalAddressUsage> = addressUsageEntities.map { from(it) }
  }
}
