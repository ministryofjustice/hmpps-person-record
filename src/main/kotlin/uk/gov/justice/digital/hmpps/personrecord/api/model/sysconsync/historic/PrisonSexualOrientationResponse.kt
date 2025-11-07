package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonSexualOrientationEntity
import java.util.UUID

data class PrisonSexualOrientationResponse(
  val cprSexualOrientationId: UUID
) {
  companion object {
    fun from(sexualOrientation: PrisonSexualOrientationEntity) = PrisonSexualOrientationResponse(
      cprSexualOrientationId = sexualOrientation.cprSexualOrientationId
    )
  }
}
