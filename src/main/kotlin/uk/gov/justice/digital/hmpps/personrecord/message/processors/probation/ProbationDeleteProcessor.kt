package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderDeleted
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.PersonDeletionService

@Component
class ProbationDeleteProcessor(
  private val personDeletionService: PersonDeletionService,
  private val personRepository: PersonRepository,
) {

  fun processEvent(event: ProbationOffenderDeleted) {
    personDeletionService.processDelete {
      personRepository.findByCrn(event.crn)
    }
  }
}
