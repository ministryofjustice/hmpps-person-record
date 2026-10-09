package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import java.time.LocalDateTime

data class CanonicalAddressUsage(
  @JsonUnwrapped
  val usageCode: CanonicalAddressUsageCode,
  @Schema(description = "Address usage active flag", example = "true")
  val isActive: Boolean,
  @Schema(description = "Date address usage was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that added address usage")
  val createUserId: String? = null,
  @Schema(description = "Date address usage was last modified", example = "2023-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that last modified address usage")
  val modifyUserId: String? = null,
) {

  companion object {
    fun from(addressUsageEntity: AddressUsageEntity): CanonicalAddressUsage = CanonicalAddressUsage(
      usageCode = CanonicalAddressUsageCode.from(addressUsageEntity.usageCode),
      isActive = addressUsageEntity.active,
      createDateTime = addressUsageEntity.createDateTime,
      createUserId = addressUsageEntity.createUserId,
      modifyDateTime = addressUsageEntity.modifyDateTime,
      modifyUserId = addressUsageEntity.modifyUserId,
    )

    fun fromAddressUsageEntityList(addressUsageEntities: List<AddressUsageEntity>): List<CanonicalAddressUsage> = addressUsageEntities.map { from(it) }
  }
}
