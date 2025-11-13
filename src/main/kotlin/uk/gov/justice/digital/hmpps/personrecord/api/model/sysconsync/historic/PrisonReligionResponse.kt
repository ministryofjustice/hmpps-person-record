package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import java.util.UUID

data class PrisonReligionResponse(
  val cprReligionId: UUID,
) {
  companion object {
    fun from(religion: PrisonReligionEntity) = PrisonReligionResponse(
      cprReligionId = religion.cprReligionId,
    )
  }
}
