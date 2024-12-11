package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person.Companion.extractSourceSystemId
import java.time.LocalDateTime

open class EventLoggingService(
  private val eventLoggingRepository: EventLoggingRepository,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
) {

  open fun recordEventLog(
    beforePerson: Person?,
    processedPerson: Person?,
    uuid: String? = null,
    eventType: String? = null,
  ) {
    val operationId = telemetryClient.context.operation.id
    val personForIdentifier = processedPerson ?: beforePerson
    val eventLog = EventLoggingEntity(
      beforeData = beforePerson?.let { objectMapper.writeValueAsString(it) },
      processedData = processedPerson?.let { objectMapper.writeValueAsString(it) },
      sourceSystemId = personForIdentifier.extractSourceSystemId(),
      uuid = uuid,
      sourceSystem = personForIdentifier?.sourceSystemType?.name,
      eventType = eventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )

    eventLoggingRepository.save(eventLog)
  }
}
