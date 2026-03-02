package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonReligionGetResponse(
  val prisonNumber: String,
  val religion: PrisonReligion,
) {
  companion object {
    fun from(prisonNumber: String, prisonReligionEntity: PrisonReligionEntity): PrisonReligionGetResponse = PrisonReligionGetResponse(
      prisonNumber = prisonNumber,
      religion = PrisonReligion(
        religionCode = prisonReligionEntity.code,
        changeReasonKnown = prisonReligionEntity.changeReasonKnown,
        comments = prisonReligionEntity.comments,
        verified = prisonReligionEntity.verified,
        startDate = prisonReligionEntity.startDate,
        endDate = prisonReligionEntity.endDate,
        modifyDateTime = prisonReligionEntity.modifyDateTime,
        modifyUserId = prisonReligionEntity.modifyUserId,
        current = prisonReligionEntity.prisonRecordType.value,
      ),
    )
  }
}

data class PrisonReligion(
  val religionCode: String? = null,
  val changeReasonKnown: Boolean? = null,
  val comments: String? = null,
  val verified: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val modifyDateTime: LocalDateTime? = null,
  val modifyUserId: String? = null,
  val current: Boolean? = null,
)
