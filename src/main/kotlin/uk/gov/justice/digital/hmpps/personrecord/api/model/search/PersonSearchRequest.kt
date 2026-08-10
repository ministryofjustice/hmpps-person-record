package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonSearchRequest(
  @Schema(required = true)
  @NotBlank
  val firstName: String,
  @Schema(required = true)
  @NotBlank
  val lastName: String,
  val middleName: String? = null,
  @Schema(required = true)
  val dateOfBirth: LocalDate,
  val firstNameAliases: List<String> = emptyList(),
  val lastNameAliases: List<String> = emptyList(),
  val dateOfBirthAliases: List<LocalDate> = emptyList(),
  val postcodes: List<String> = emptyList(),
)
