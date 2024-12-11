package uk.gov.justice.digital.hmpps.personrecord.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

class EventLoggingServiceNoOp(
  eventLoggingRepository: EventLoggingRepository,
  telemetryClient: TelemetryClient,
  objectMapper: ObjectMapper,
) : EventLoggingService(eventLoggingRepository, telemetryClient, objectMapper) {

  override fun recordEventLog(
    beforePerson: Person?,
    processedPerson: Person?,
    uuid: String?,
    eventType: String?,
  ) {
    log.trace("event logging disabled")
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
