package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import java.time.LocalDateTime
import java.util.UUID

data class RecordEventLog(
  val eventType: CPRLogEvents,
  val personEntity: PersonEntity,
  val clusterDetail: EventLogClusterDetail? = null,
  val eventTimestamp: LocalDateTime = LocalDateTime.now(),
)

data class EventLogClusterDetail(
  val personUUID: UUID?,
  val status: UUIDStatusType?,
) {

  companion object {

    fun from(personKeyEntity: PersonKeyEntity?) = EventLogClusterDetail(
      personUUID = personKeyEntity?.personUUID,
      status = personKeyEntity?.status,
    )
  }
}
