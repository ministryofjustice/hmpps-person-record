package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonImmigrationStatusEntity
import java.util.UUID

data class PrisonImmigrationStatusResponse(
  val cprImmigrationStatusId: UUID,
) {
  companion object {
    fun from(prisonImmigrationStatusEntity: PrisonImmigrationStatusEntity) = PrisonImmigrationStatusResponse(
      cprImmigrationStatusId = prisonImmigrationStatusEntity.cprImmigrationStatusId,
    )
  }
}
