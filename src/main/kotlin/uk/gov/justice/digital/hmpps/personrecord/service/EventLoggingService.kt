package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDateTime

@Service
class EventLoggingService(
  private val eventLoggingRepository: EventLoggingRepository,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
) {

  fun recordEventLog(
    beforePerson: Person?,
    processedPerson: Person?,
    uuid: String? = null,
    eventType: String? = null,
  ): EventLoggingEntity {
    val operationId = telemetryClient.context.operation.id
    val personForIdentifier = processedPerson ?: beforePerson
    val eventLog = EventLoggingEntity(
      beforeData = beforePerson?.let { objectMapper.writeValueAsString(it) },
      processedData = processedPerson?.let { objectMapper.writeValueAsString(it) },
      sourceSystemId = extractSourceSystemId(personForIdentifier),
      uuid = uuid,
      sourceSystem = personForIdentifier?.sourceSystemType?.name,
      eventType = eventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )

    return eventLoggingRepository.save(eventLog)
  }

  private fun extractSourceSystemId(person: Person?): String? {
    return when (person?.sourceSystemType) {
      DELIUS -> person.crn
      NOMIS -> person.prisonNumber
      COMMON_PLATFORM -> person.defendantId
      else -> null
    }
  }
}
