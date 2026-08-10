package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "PersonSearchRequest")
data class PersonSearchRequest(
  @Schema(required = true)
  val firstName: String,
  @Schema(required = true)
  val lastName: String,
  val middleName: String? = null,
  @Schema(required = true)
  val dateOfBirth: LocalDate,
  val firstNameAliases: List<String>? = null,
  val lastNameAliases: List<String>? = null,
  val dateOfBirthAliases: List<LocalDate>? = null,
  val postcodes: List<String>? = null,
)
