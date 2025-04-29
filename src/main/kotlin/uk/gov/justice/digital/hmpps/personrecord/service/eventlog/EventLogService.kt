package uk.gov.justice.digital.hmpps.personrecord.service.eventlog

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository

@Component
class EventLogService(
  private val eventLogRepository: EventLogRepository,
  private val objectMapper: ObjectMapper,
) {

  fun logEvent(
    personEntity: PersonEntity,
    eventType: CPRLogEvents,
    personKeyEntity: PersonKeyEntity? = null,
    clusterComposition: List<ValidCluster>? = null,
  ): EventLogEntity = eventLogRepository.save(
    EventLogEntity.from(
      personEntity,
      eventType,
      objectMapper.writeValueAsString(clusterComposition),
      personKeyEntity,
    ),
  )
}
