package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED

@Component
class ProbationUnmergeEventProcessor(
  val telemetryService: TelemetryService,
) : BaseProbationEventProcessor() {

  fun processEvent(domainEvent: DomainEvent) {
    telemetryService.trackEvent(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf(
        EventKeys.REACTIVATED_CRN to domainEvent.additionalInformation?.reactivatedCRN,
        EventKeys.UNMERGED_CRN to domainEvent.additionalInformation?.unmergedCRN,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to DELIUS.name,
      ),
    )
    getProbationCase(domainEvent.additionalInformation?.unmergedCRN!!).fold(
      onSuccess = {
        log.info("Successfully mapped unmerged record")
      },
      onFailure = {
        log.error("Cannot find merge target CRN  within Delius: ${it.message}")
        throw it
      },
    )
  }
}
