package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

data class ReclusterRecord(
  @JsonProperty("source_system")
  val sourceSystem: SourceSystemType,
  @JsonProperty("source_system_id")
  val sourceSystemId: String,
)
