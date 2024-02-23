package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.OffenderDetailRestClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.ProbationCaseEngagementService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.net.URI

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"

@Component(value = NEW_OFFENDER_CREATED)
class OffenderCreatedEventProcessor(
  val telemetryService: TelemetryService,
  val offenderDetailRestClient: OffenderDetailRestClient,
  val probationCaseEngagementService: ProbationCaseEngagementService,
) : EventProcessor() {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun processEvent(domainEvent: DomainEvent) {
    val offenderDetailUrl = domainEvent.detailUrl
    val crnIdentifier = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }
    telemetryService.trackEvent(
      TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED,
      mapOf("CRN" to crnIdentifier?.value),
    )
    log.debug("Entered processEvent with  url $offenderDetailUrl")
    getNewOffenderDetail(offenderDetailUrl).fold(
      onSuccess = { deliusOffenderDetail ->
        deliusOffenderDetail?.let(probationCaseEngagementService::processNewOffender)
      },
      onFailure = {
        log.error("Error retrieving new offender detail: ${it.message}")
        telemetryService.trackEvent(
          TelemetryEventType.DELIUS_OFFENDER_READ_FAILURE,
          mapOf("CRN" to crnIdentifier?.value),
        )
        throw it
      },
    )
  }
  private fun getNewOffenderDetail(offenderDetailsUrl: String): Result<DeliusOffenderDetail?> {
    return try {
      Result.success(offenderDetailRestClient.getNewOffenderDetail(URI.create(offenderDetailsUrl).path))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
