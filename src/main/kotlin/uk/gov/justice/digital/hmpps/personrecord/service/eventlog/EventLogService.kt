package uk.gov.justice.digital.hmpps.personrecord.service.eventlog

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository

@Component
class EventLogService(
  private val eventLogRepository: EventLogRepository,
) {

  fun logEvent(
    personEntity: PersonEntity,
    eventType: CPRLogEvents,
    personKeyEntity: PersonKeyEntity? = null,
  ): EventLogEntity = eventLogRepository.save(EventLogEntity.from(personEntity, eventType, personKeyEntity = personKeyEntity))
}
