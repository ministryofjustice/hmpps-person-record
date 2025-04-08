package uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class IsClusterValidMissingRecordResponse(
  val unknownIds: List<String>
)
