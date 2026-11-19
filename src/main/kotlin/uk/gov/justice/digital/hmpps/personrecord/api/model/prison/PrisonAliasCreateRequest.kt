package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import java.time.LocalDate

@Schema(description = "Create a prisoner pseudonym")
data class PrisonAliasCreateRequest(
  @Schema(description = "Prisoner's first name", example = "John", requiredMode = REQUIRED)
  @field:NotBlank
  @field:Pattern(regexp = "^[A-Z|a-z ,.'-]+$", message = "First name is not valid")
  val firstName: String,

  @Schema(description = "Prisoner's middle names", example = "Middleone Middletwo")
  @field:Pattern(regexp = "^[A-Z|a-z ,.'-]+$", message = "Middle names are not valid")
  val middleNames: String? = null,

  @Schema(description = "Prisoner's last name", example = "Smith", requiredMode = REQUIRED)
  @field:NotBlank
  @field:Pattern(regexp = "^[A-Z|a-z ,.'-]+$", message = "Last name is not valid")
  val lastName: String,

  @Schema(
    requiredMode = REQUIRED,
    description = "The prisoner's date of birth. Must be specified in YYYY-MM-DD format. Range allowed is 16-110 years",
    example = "1970-01-01",
  )
  val dateOfBirth: LocalDate,

  @Schema(description = "The prisoner's sex", requiredMode = REQUIRED, example = "F")
  val sex: SexCode,

  @Schema(description = "The prisoner's title", example = "MR")
  val title: TitleCode? = null,

  @Schema(description = "The prisoner's ethnicity", example = "W1")
  val ethnicity: EthnicityCode? = null,

  @Schema(description = "The name type", example = "CN")
  val nameType: PrisonAliasNameType? = null,

  @Schema(description = "Boolean flag to indicate if the alias is the primary name", example = "true", requiredMode = REQUIRED)
  val isPrimary: Boolean,

  @Schema(description = "The user who made the change", requiredMode = REQUIRED)
  val userId: String,
)
