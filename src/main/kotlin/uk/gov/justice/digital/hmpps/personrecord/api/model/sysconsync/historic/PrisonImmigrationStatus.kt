package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonImmigrationStatus(
  @Schema(description = "Flag indicating whether data is an interest to immigration", example = "true", required = true)
  val interestToImmigration: Boolean,
  @Schema(description = "The immigration status modify date and time", example = "2000-01-01 12:00:00", required = true)
  @NotBlank
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The immigration status modify user id", example = "12345", required = true)
  @NotBlank
  val modifyUserId: String? = null,
)
