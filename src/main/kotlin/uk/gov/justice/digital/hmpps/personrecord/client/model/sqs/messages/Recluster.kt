package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Recluster(
  val uuid: String?,
)
