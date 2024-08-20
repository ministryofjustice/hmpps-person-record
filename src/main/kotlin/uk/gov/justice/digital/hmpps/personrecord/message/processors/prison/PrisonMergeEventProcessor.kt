package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED

@Component
class PrisonMergeEventProcessor(
  val telemetryService: TelemetryService,
  val personRepository: PersonRepository,

) : BasePrisonEventProcessor() {

  fun processEvent(domainEvent: DomainEvent) {
    telemetryService.trackEvent(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        EventKeys.SOURCE_PRISON_NUMBER to domainEvent.additionalInformation?.prisonNumber,
        EventKeys.TARGET_PRISON_NUMBER to domainEvent.additionalInformation?.sourcePrisonNumber,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name,
      ),
    )
    getPrisonerDetails(domainEvent.additionalInformation?.sourcePrisonNumber!!).fold(
      onSuccess = {
        log.info("Successfully mapped merge record")
      },
      onFailure = {
        log.error("Error retrieving prisoner detail: ${it.message}")
        throw it
      },
    )
  }
}
