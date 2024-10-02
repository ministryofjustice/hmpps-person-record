package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED

@Component
class ProbationEventProcessor(
  val telemetryService: TelemetryService,
  val personService: PersonService,
  val personRepository: PersonRepository,
) : BaseProbationEventProcessor() {

  fun processEvent(crn: String, eventType: String) = runBlocking {
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(EventKeys.CRN to crn, EventKeys.EVENT_TYPE to eventType, EventKeys.SOURCE_SYSTEM to DELIUS.name),
    )
    getProbationCase(crn).fold(
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
