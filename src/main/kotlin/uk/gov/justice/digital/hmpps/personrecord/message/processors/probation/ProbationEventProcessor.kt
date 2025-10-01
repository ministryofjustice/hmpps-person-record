package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class ProbationEventProcessor(
  private val probationProcessor: ProbationProcessor,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(person: Person) {
    probationProcessor.processProbationEvent(person)
  }
}
