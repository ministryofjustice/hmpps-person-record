package uk.gov.justice.digital.hmpps.personrecord.api.model.admin

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

data class AdminDeleteRecord(
  @JsonProperty("source_system")
  val sourceSystem: SourceSystemType,
  @JsonProperty("source_system_id")
  val sourceSystemId: String,
)
