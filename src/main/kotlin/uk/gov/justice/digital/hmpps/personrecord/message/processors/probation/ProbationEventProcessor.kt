package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class ProbationEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val encodingService: EncodingService,
) {

  fun processEvent(crn: String) {
    encodingService.getProbationCase(
      crn,
    ) {
      it?.let {
        createUpdateService.processPerson(Person.from(it)) {
          personRepository.findByCrn(crn)
        }
      }
    }
  }
}
