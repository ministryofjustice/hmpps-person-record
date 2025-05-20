package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class PersonProcessorFactory(
  private val personRepository: PersonRepository,
) {

  fun init() = PersonProcessorChain(
    context = PersonContext(),
    personRepository = personRepository,
  )

}