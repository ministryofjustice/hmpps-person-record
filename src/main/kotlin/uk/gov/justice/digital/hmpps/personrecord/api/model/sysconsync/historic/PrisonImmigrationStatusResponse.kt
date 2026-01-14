package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import java.util.UUID

data class PrisonImmigrationStatusResponse(
  val cprImmigrationStatusId: UUID? = null,
) {
  companion object {
    fun from(cprImmigrationStatusId: UUID?) = PrisonImmigrationStatusResponse(
      cprImmigrationStatusId = cprImmigrationStatusId,
    )
  }
}
