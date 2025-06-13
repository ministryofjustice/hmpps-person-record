package uk.gov.justice.digital.hmpps.personrecord.api.model.admin

import java.util.UUID

data class AdminReclusterRequest(
  val clusters: List<UUID>,
)
