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
        EventKeys.SOURCE_CRN to domainEvent.additionalInformation?.sourceCrn,
        EventKeys.TARGET_CRN to domainEvent.additionalInformation?.targetCrn,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to DELIUS.name,
      ),
    )
    getProbationCase(domainEvent.additionalInformation?.targetCrn!!).fold(
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
