package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonReligion(
  val religionCode: ReligionCode,
  val religionDescription: String?,
  val changeReasonKnown: Boolean,
  val comments: String? = null,
  val startDate: LocalDate,
  val endDate: LocalDate? = null,
  val modifyDateTime: LocalDateTime? = null,
  val modifyUserId: String? = null,
  val current: Boolean,
  val createDateTime: LocalDateTime,
  val createUserId: String,
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
    )
  }
}
