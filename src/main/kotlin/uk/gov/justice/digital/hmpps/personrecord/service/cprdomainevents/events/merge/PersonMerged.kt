package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail

data class PersonMerged(
  val from: PersonEntity?,
  val fromClusterDetail: EventLogClusterDetail,
  val to: PersonEntity,
)
