package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.toReligionCodeDescription

data class PrisonReligionGetResponse(
  val prisonNumber: String,
  val religion: PrisonReligionGet,
) {
  companion object {
    fun from(prisonNumber: String, prisonReligionEntity: PrisonReligionEntity): PrisonReligionGetResponse = PrisonReligionGetResponse(
      prisonNumber = prisonNumber,
      religion = PrisonReligionGet(
        religionCode = prisonReligionEntity.code,
        religionDescription = prisonReligionEntity.code.toReligionCodeDescription(),
        changeReasonKnown = prisonReligionEntity.changeReasonKnown,
        comments = prisonReligionEntity.comments,
        startDate = prisonReligionEntity.startDate,
        endDate = prisonReligionEntity.endDate,
        modifyDateTime = prisonReligionEntity.modifyDateTime,
        modifyUserId = prisonReligionEntity.modifyUserId,
        current = prisonReligionEntity.prisonRecordType.value,
      ),
    )
  }
}
