package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import java.time.LocalDateTime

data class RecordEventLog(
  val eventType: CPRLogEvents,
  val personEntity: PersonEntity,
  val personKeyEntity: PersonKeyEntity? = null,
  val eventTimestamp: LocalDateTime = LocalDateTime.now(),
)
