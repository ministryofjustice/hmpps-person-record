package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.toReligionCodeDescription
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonReligionGet(
  val religionCode: String? = null,
  val religionDescription: String? = null,
  val changeReasonKnown: Boolean? = null,
  val comments: String? = null,
  val verified: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val modifyDateTime: LocalDateTime? = null,
  val modifyUserId: String? = null,
  val current: Boolean? = null,
) {
  companion object {
    fun from(prisonReligionEntity: PrisonReligionEntity): PrisonReligionGet = PrisonReligionGet(
      religionCode = prisonReligionEntity.code,
      religionDescription = prisonReligionEntity.code.toReligionCodeDescription(),
      changeReasonKnown = prisonReligionEntity.changeReasonKnown,
      comments = prisonReligionEntity.comments,
      verified = prisonReligionEntity.verified,
      startDate = prisonReligionEntity.startDate,
      endDate = prisonReligionEntity.endDate,
      modifyDateTime = prisonReligionEntity.modifyDateTime,
      modifyUserId = prisonReligionEntity.modifyUserId,
      current = prisonReligionEntity.prisonRecordType.value,
    )
  }
}
