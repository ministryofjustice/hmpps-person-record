package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonNationalityEntity
import java.util.UUID

data class PrisonNationalityResponse(
  val cprNationalityId: UUID,
) {
  companion object {
    fun from(nationality: PrisonNationalityEntity) = PrisonNationalityResponse(
      cprNationalityId = nationality.cprNationalityId,
    )
  }
}
