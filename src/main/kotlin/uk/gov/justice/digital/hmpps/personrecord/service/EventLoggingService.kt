package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import java.time.LocalDateTime

@Service
class EventLoggingService(
  private val eventLoggingRepository: EventLoggingRepository,
  private val telemetryClient: TelemetryClient,

) {

  fun mapToEventLogging(
    beforeData: String? = null,
    processedData: String? = null,
    sourceSystemId: String? = null,
    uuid: String? = null,
    sourceSystem: String? = null,
    eventType: String? = null,
    eventTimeStamp: LocalDateTime? = null,
  ): EventLoggingEntity {
    val operationId = telemetryClient.context.operation.id

    val eventLog = EventLoggingEntity(
      beforeData = beforeData,
      processedData = processedData,
      sourceSystemId = sourceSystemId,
      uuid = uuid,
      sourceSystem = sourceSystem,
      eventType = eventType,
      eventTimestamp = eventTimeStamp,
      operationId = operationId,
    )

    return eventLoggingRepository.save(eventLog)
  }
}
