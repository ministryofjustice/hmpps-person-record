package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import java.time.LocalDateTime

@Service
class EventLoggingService(
  private val eventLoggingRepository: EventLoggingRepository,

) {

  fun mapToEventLogging(
    operationId: String? = null,
    beforeData: String? = null,
    processedData: String? = null,
    sourceSystemId: String? = null,
    uuid: String? = null,
    sourceSystem: String? = null,
    messageEventType: String? = null,
  ): EventLoggingEntity {
    val eventLog = EventLoggingEntity(
      beforeData = beforeData,
      processedData = processedData,
      sourceSystemId = sourceSystemId,
      uuid = uuid,
      sourceSystem = sourceSystem,
      messageEventType = messageEventType,
      eventTimestamp = LocalDateTime.now(),
      operationId = operationId,
    )

    return eventLoggingRepository.save(eventLog)
  }
}
