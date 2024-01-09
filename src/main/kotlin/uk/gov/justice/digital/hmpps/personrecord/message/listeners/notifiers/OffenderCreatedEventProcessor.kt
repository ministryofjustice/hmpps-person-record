package uk.gov.justice.digital.hmpps.personrecord.message.listeners.notifiers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.OffenderDetailRestClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.net.URI
import kotlin.math.log

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"

@Component(value = NEW_OFFENDER_CREATED)
class OffenderCreatedEventProcessor(
  val telemetryService: TelemetryService,
  val offenderDetailRestClient: OffenderDetailRestClient,
) : EventProcessor() {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun processEvent(domainEvent: DomainEvent) {
    val offenderDetailUrl = domainEvent.detailUrl
    val path = URI.create(offenderDetailUrl).path
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }
    telemetryService.trackEvent(
      TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED,
      mapOf("CRN" to crn?.value),
    )
    getNewOffenderDetail(offenderDetailUrl).fold(
      onSuccess = {
        // TODO process new offender (CPR-112)
      },
      onFailure = {
        log.error("Error retrieving new offender detail: ${it.message}")
        throw it
      },
    )
    LOG.debug("Entered processEvent with  urlR:$offenderDetailUrl")
  }

  private fun getNewOffenderDetail(offenderDetailUrl: String): Result<DeliusOffenderDetail?> {
    return try {
      Result.success(offenderDetailRestClient.getNewOffenderDetail(URI.create(offenderDetailUrl).path))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
