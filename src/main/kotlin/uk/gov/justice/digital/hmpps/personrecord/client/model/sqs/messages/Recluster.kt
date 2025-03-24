package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class Recluster(
  val uuid: UUID?,
  val changedRecordId: Long?,
)
