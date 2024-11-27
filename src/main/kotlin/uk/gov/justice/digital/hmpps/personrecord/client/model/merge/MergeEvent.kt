package uk.gov.justice.digital.hmpps.personrecord.client.model.merge

import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys

data class MergeEvent(
  val event: String,
  val sourceSystemId: Pair<EventKeys, String>,
  val targetSystemId: Pair<EventKeys, String>,
  val mergedRecord: Person,
)
