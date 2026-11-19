package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import java.time.LocalDate

@Schema(description = "A prisoner alias")
data class PrisonAliasResponse(
  @Schema(description = "The prison number for this alias", example = "A1234AA")
  val prisonNumber: String,
  @Schema(description = "The alias id", example = "4f2b1d8a-9f3c-4a1f-8b2d-112233445566")
  val cprAliasId: String,
  @Schema(description = "Boolean flag to indicate if the alias is the primary name", example = "true")
  val isPrimary: Boolean,
  @Schema(description = "First name", example = "John")
  val firstName: String,
  @Schema(description = "Middle names", example = "Middleone Middletwo")
  val middleNames: String? = null,
  @Schema(description = "Last name", example = "Smith")
  val lastName: String,
  @Schema(description = "Date of birth", example = "1980-02-28")
  val dateOfBirth: LocalDate,
  @Schema(description = "Name type", example = "CN")
  val nameType: PrisonAliasNameType? = null,
  @Schema(description = "Title")
  val title: CanonicalTitle? = null,
  @Schema(description = "Sex")
  val sex: CanonicalSex? = null,
  @Schema(description = "Ethnicity")
  val ethnicity: CanonicalEthnicity? = null,
)
