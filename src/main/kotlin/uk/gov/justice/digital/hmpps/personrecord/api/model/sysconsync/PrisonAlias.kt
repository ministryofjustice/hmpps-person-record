package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonAlias(
  @Schema(description = "The nomis offender id for this alias", example = "10000")
  val nomisOffenderId: Long,
  @Schema(description = "The cpr alias id", example = "4f2b1d8a-9f3c-4a1f-8b2d-112233445566")
  val cprAliasId: String? = null,
  @Schema(description = "The alias title code", example = "MR")
  val titleCode: TitleCode? = null,
  @Schema(description = "The alias first name", example = "Jon")
  val firstName: String? = null,
  @Schema(description = "The alias middle names", example = "James")
  val middleNames: String? = null,
  @Schema(description = "The alias last name", example = "Smythe")
  val lastName: String? = null,
  @Schema(description = "The alias date of birth", example = "1980-01-01")
  val dateOfBirth: LocalDate? = null,
  @Schema(description = "The alias sex code", example = "M")
  val sexCode: SexCode? = null,
  @Schema(description = "Indicates if this is a primary alias", example = "true")
  val isPrimary: Boolean,
  @Schema(description = "Birth place of this offender record")
  val birthPlace: String?,
  @Schema(description = "Birth country of this offender record")
  val birthCountry: CountryCode?,
  @Schema(description = "Ethnicity of this offender record")
  val ethnicity: EthnicityCode?,
  @Schema(description = "List of identifiers associated with this alias")
  val identifiers: List<PrisonIdentifier>,
  @Schema(description = "Suffix")
  val suffix: PrisonPseudonymSuffix?,
  @Schema(description = "Name type")
  val nameType: PrisonAliasNameType?,
  @Schema(description = "The alias create date and time", example = "2000-01-01T12:00:00", required = true)
  val createDateTime: LocalDateTime,
  @Schema(description = "The alias create user id", example = "12345", required = true)
  val createUserId: String,
  @Schema(description = "The alias modify date and time", example = "2000-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The alias modify user id", example = "12345")
  val modifyUserId: String? = null,
)
