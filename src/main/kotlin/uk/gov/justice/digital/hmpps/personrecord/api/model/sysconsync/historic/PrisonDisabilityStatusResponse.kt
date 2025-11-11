package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonDisabilityStatusEntity
import java.util.UUID

data class PrisonDisabilityStatusResponse(
  val cprDisabilityStatusId: UUID,
) {
  companion object {
    fun from(disabilityStatus: PrisonDisabilityStatusEntity) = PrisonDisabilityStatusResponse(
      cprDisabilityStatusId = disabilityStatus.cprDisabilityStatusId,
    )
  }
}
