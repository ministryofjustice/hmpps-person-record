package uk.gov.justice.digital.hmpps.personrecord.service

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

) {

  fun recordEventLog(
    beforeData: String? = null,
    processedData: String? = null,
    uuid: String? = null,
    sourceSystem: String? = null,
    messageEventType: String? = null,
    processedPerson: Person?,
  ): EventLoggingEntity {
    val operationId = telemetryClient.context.operation.id

    val eventLog = EventLoggingEntity(
      beforeData = beforeData,
      processedData = processedData,
      sourceSystemId = extractSourceSystemId(processedPerson),
      uuid = uuid,
      sourceSystem = sourceSystem,
      messageEventType = messageEventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )

    return eventLoggingRepository.save(eventLog)
  }

  private fun extractSourceSystemId(personEntity: Person?): String? {
    return when (personEntity?.sourceSystemType) {
      DELIUS -> personEntity.crn
      NOMIS -> personEntity.prisonNumber
      COMMON_PLATFORM -> personEntity.defendantId
      else -> null
    }
  }
}
