package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.DeletionService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED

@Component
class ProbationDeleteProcessor(
  private val telemetryService: TelemetryService,
  private val deletionService: DeletionService,
  private val personRepository: PersonRepository,
) {

  fun processEvent(crn: String, eventType: String) {
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(EventKeys.CRN to crn, EventKeys.EVENT_TYPE to eventType, EventKeys.SOURCE_SYSTEM to DELIUS.name),
    )
    deletionService.processDelete {
      personRepository.findByCrn(crn)
    }
  }
}
