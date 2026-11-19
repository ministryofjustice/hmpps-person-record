package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonReligion(
  @Schema(description = "Religion code", example = "SCIE")
  val religionCode: ReligionCode,
  @Schema(description = "Description associated with the religion code", example = "Scientologist")
  val religionDescription: String?,
  @Schema(description = "Was a reason given for change of religion?")
  val changeReasonKnown: Boolean,
  @Schema(description = "Comments describing reason for change of religion")
  val comments: String? = null,
  @Schema(description = "Date the religion started", example = "2024-01-01")
  val startDate: LocalDate,
  @Schema(description = "Date the religion ended", example = "2024-12-12")
  val endDate: LocalDate? = null,
  @Schema(description = "Date religion was modified", example = "2024-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that modified religion")
  val modifyUserId: String? = null,
  @Schema(description = "Indicates if the religion is current")
  val current: Boolean,
  @Schema(description = "Date religion was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime,
  @Schema(description = "Username of staff member that added religion")
  val createUserId: String,
  @Schema(description = "Unique identifier for the religion")
  val cprReligionId: String? = null,
) {
  companion object {
    fun from(prisonReligionEntity: PrisonReligionEntity): PrisonReligion = PrisonReligion(
      religionCode = prisonReligionEntity.code,
      religionDescription = prisonReligionEntity.code.description,
      changeReasonKnown = prisonReligionEntity.changeReasonKnown,
      comments = prisonReligionEntity.comments,
      startDate = prisonReligionEntity.startDate,
      endDate = prisonReligionEntity.endDate,
      modifyDateTime = prisonReligionEntity.modifyDateTime,
      modifyUserId = prisonReligionEntity.modifyUserId,
      current = prisonReligionEntity.prisonRecordType.value,
      createDateTime = prisonReligionEntity.createDateTime,
      createUserId = prisonReligionEntity.createUserId,
      cprReligionId = prisonReligionEntity.updateId?.toString(),
    )
  }
}
