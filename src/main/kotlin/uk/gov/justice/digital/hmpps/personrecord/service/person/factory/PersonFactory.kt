package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonClusterProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonCreateProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonLogProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonSearchProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonUpdateProcessor

@Component
class PersonFactory(
  private val personCreateProcessor: PersonCreateProcessor,
  private val personUpdateProcessor: PersonUpdateProcessor,
  private val personSearchProcessor: PersonSearchProcessor,
  private val personClusterProcessor: PersonClusterProcessor,
  private val personLogProcessor: PersonLogProcessor,
) {

  fun from(person: Person) = PersonProcessorChain(
    context = PersonContext(person = person),
    personCreateProcessor = personCreateProcessor,
    personUpdateProcessor = personUpdateProcessor,
    personSearchProcessor = personSearchProcessor,
    personClusterProcessor = personClusterProcessor,
    personLogProcessor = personLogProcessor,
  )
}
