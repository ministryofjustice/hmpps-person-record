package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.DeletionService

@Component
class ProbationDeleteProcessor(
  private val deletionService: DeletionService,
  private val personRepository: PersonRepository,
) {

  fun processEvent(crn: String, eventType: String) {
    deletionService.processDelete(eventType) {
      personRepository.findByCrn(crn)
    }
  }
}
