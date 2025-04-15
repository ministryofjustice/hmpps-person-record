package uk.gov.justice.digital.hmpps.personrecord.service.eventlog

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import java.util.UUID

@Component
class EventLogService(
  private val eventLogRepository: EventLogRepository,
  private val telemetryClient: TelemetryClient,
) {

  fun logEvent(
    personEntity: PersonEntity,
    eventType: CPRLogEvents,
    personKeyEntity: PersonKeyEntity? = null,
  ): EventLogEntity = eventLogRepository.save(EventLogEntity.from(personEntity, eventType, getOperationId(), personKeyEntity = personKeyEntity))

  private fun getOperationId() = telemetryClient.context.operation.id ?: UUID.randomUUID().toString()
}
