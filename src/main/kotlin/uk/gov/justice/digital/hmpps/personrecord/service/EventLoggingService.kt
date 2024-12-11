package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.extractSourceSystemId
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.CPR
import java.time.LocalDateTime
import java.util.UUID

@Component
class EventLoggingService(
  private val eventLoggingRepository: EventLoggingRepository,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
) {

  fun snapshotPersonEntity(personEntity: PersonEntity) = snapshotEntity<PersonEntity>(personEntity)

  fun snapshotPersonKeyEntity(personKeyEntity: PersonKeyEntity) = snapshotEntity<PersonKeyEntity>(personKeyEntity)

  fun recordEventLog(
    beforePersonEntity: PersonEntity?,
    afterPersonEntity: PersonEntity?,
    uuid: UUID?,
    eventType: String? = null,
  ): EventLoggingEntity {
    val personEntityForIdentifier = afterPersonEntity ?: beforePersonEntity
    val operationId = telemetryClient.context.operation.id
    val eventLog = EventLoggingEntity(
      beforeData = beforePersonEntity?.let { objectMapper.writeValueAsString(it) },
      processedData = afterPersonEntity?.let { objectMapper.writeValueAsString(it) },
      sourceSystemId = personEntityForIdentifier.extractSourceSystemId(),
      uuid = uuid.toString(),
      sourceSystem = personEntityForIdentifier?.sourceSystem?.name,
      eventType = eventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )
    return eventLoggingRepository.save(eventLog)
  }

  fun recordEventLog(
    beforePersonKey: PersonKeyEntity?,
    afterPersonKey: PersonKeyEntity?,
    uuid: UUID,
    eventType: String? = null,
  ): EventLoggingEntity {
    val operationId = telemetryClient.context.operation.id
    val eventLog = EventLoggingEntity(
      beforeData = beforePersonKey?.let { objectMapper.writeValueAsString(it) },
      processedData = afterPersonKey?.let { objectMapper.writeValueAsString(it) },
      sourceSystemId = null,
      uuid = uuid.toString(),
      sourceSystem = CPR.name,
      eventType = eventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )
    return eventLoggingRepository.save(eventLog)
  }

  private inline fun <reified T> snapshotEntity(entity: Any): T = objectMapper.readValue(objectMapper.writeValueAsString(entity))
}
