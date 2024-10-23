package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED

@Component
class ProbationEventProcessor(
  private val telemetryService: TelemetryService,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
  private val encodingService: EncodingService,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(crn: String, eventType: String) {
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(EventKeys.CRN to crn, EventKeys.EVENT_TYPE to eventType, EventKeys.SOURCE_SYSTEM to DELIUS.name),
    )
    encodingService.getProbationCase(crn).fold(
      onSuccess = {
        it?.let {
          personService.processMessage(Person.from(it), eventType) {
            personRepository.findByCrn(crn)
          }
        }
      },
      onFailure = {
        log.error("Error retrieving new offender detail: ${it.message}")
        throw it
      },
    )
  }
}
