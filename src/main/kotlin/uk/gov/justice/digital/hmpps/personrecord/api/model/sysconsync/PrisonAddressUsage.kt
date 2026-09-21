package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import java.time.LocalDateTime

data class PrisonAddressUsage(
  @Schema(description = "The nomis address usage id", example = "5678")
  val nomisAddressUsageId: Long,
  @Schema(description = "The address usage code", example = "DSH")
  val addressUsageCode: AddressUsageCode,
  @Schema(description = "Is the address active", example = "true")
  val isActive: Boolean,
  @Schema(description = "The religion create date and time", example = "2000-01-01 12:00:00", required = true)
  val createDateTime: LocalDateTime,
  @Schema(description = "The religion create user id", example = "12345", required = true)
  val createUserId: String,
  @Schema(description = "The religion modify date and time", example = "2000-01-01 12:00:00", required = false)
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The religion modify user id", example = "12345", required = false)
  val modifyUserId: String? = null,
)
