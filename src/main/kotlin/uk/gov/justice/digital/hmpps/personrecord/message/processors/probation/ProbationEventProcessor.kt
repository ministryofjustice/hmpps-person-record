package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class ProbationEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  fun processEvent(crn: String) {
    corePersonRecordAndDeliusClient.getProbationCase(crn).let {
      createUpdateService.processPerson(Person.from(it)) {
        personRepository.findByCrn(crn)
      }
    }
  }
}
