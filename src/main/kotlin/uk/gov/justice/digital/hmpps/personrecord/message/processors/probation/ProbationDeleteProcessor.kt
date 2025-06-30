package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.DeletionService

@Component
class ProbationDeleteProcessor(
  private val deletionService: DeletionService,
  private val personRepository: PersonRepository,
) {

  fun processEvent(domainEvent: DomainEvent) {
    val crn = domainEvent.getCrn()
    deletionService.processDelete {
      personRepository.findByCrn(crn)
    }
  }
}
