package uk.gov.justice.digital.hmpps.personrecord.client.model.merge

import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys

data class UnmergeEvent(
  val event: String,
  val unmergedSystemId: Pair<EventKeys, String>,
  val reactivatedSystemId: Pair<EventKeys, String>,
  val unmergedRecord: Person,
  val reactivatedRecord: Person,
)
