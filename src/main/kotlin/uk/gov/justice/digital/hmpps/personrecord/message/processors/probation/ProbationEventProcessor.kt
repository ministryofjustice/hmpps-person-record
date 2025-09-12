package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class ProbationEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(crn: String) {
    corePersonRecordAndDeliusClient.getPerson(crn).let {
      createUpdateService.processPerson(it) {
        personRepository.findByCrn(crn)
      }
    }
  }
}
