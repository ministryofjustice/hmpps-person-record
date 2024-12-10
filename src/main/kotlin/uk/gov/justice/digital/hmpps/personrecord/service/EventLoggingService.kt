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
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.CPR
import java.time.LocalDateTime

@Component
class EventLoggingService(
  private val eventLoggingRepository: EventLoggingRepository,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
) {

  fun snapshotPersonEntity(personEntity: PersonEntity) = snapshotEntity<PersonEntity>(personEntity)

  fun snapshotPersonKeyEntity(personKeyEntity: PersonKeyEntity) = snapshotEntity<PersonKeyEntity>(personKeyEntity)

  fun recordEventLog(
    beforePerson: PersonEntity?,
    afterPerson: PersonEntity?,
    eventType: String? = null,
  ): EventLoggingEntity {
    val personEntityForIdentifier = afterPerson ?: beforePerson
    return saveEventLog(
      beforeData = beforePerson?.let { objectMapper.writeValueAsString(it) },
      afterData = afterPerson?.let { objectMapper.writeValueAsString(it) },
      sourceSystemId = personEntityForIdentifier.extractSourceSystemId(),
      uuid = personEntityForIdentifier?.personKey?.personId.toString(),
      sourceSystem = personEntityForIdentifier?.sourceSystem,
      eventType = eventType,
    )
  }

  fun recordEventLog(
    beforePersonKey: PersonKeyEntity?,
    afterPersonKey: PersonKeyEntity?,
    eventType: String? = null,
  ): EventLoggingEntity {
    val personKeyForIdentifier = beforePersonKey ?: afterPersonKey
    return saveEventLog(
      beforeData = beforePersonKey?.let { objectMapper.writeValueAsString(it) },
      afterData = afterPersonKey?.let { objectMapper.writeValueAsString(it) },
      sourceSystemId = null,
      uuid = personKeyForIdentifier?.personId.toString(),
      eventType = eventType,
    )
  }

  private fun saveEventLog(
    beforeData: String?,
    afterData: String?,
    eventType: String?,
    uuid: String,
    sourceSystemId: String?,
    sourceSystem: SourceSystemType? = CPR,
  ): EventLoggingEntity {
    val operationId = telemetryClient.context.operation.id
    val eventLog = EventLoggingEntity(
      beforeData = beforeData,
      processedData = afterData,
      sourceSystemId = sourceSystemId,
      uuid = uuid,
      sourceSystem = sourceSystem?.name,
      eventType = eventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )
    return eventLoggingRepository.save(eventLog)
  }

  private inline fun <reified T> snapshotEntity(entity: Any): T = objectMapper.readValue(objectMapper.writeValueAsString(entity))
}
