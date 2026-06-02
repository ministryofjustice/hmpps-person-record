package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import kotlin.reflect.KClass

@Component
class ProbationEventProcessor(
  private val probationProcessor: ProbationProcessor,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(person: Person, childrenToIgnore: Set<KClass<*>> = emptySet()) {
    probationProcessor.processProbationEvent(person, childrenToIgnore)
  }
}
