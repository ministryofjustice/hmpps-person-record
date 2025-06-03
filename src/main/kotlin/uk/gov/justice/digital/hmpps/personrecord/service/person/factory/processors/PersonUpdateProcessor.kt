package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext

@Component
class PersonUpdateProcessor(
  private val personRepository: PersonRepository,
) {

  fun updatePersonEntity(context: PersonContext): PersonUpdateProcessor {
    context.personEntity?.let {
      val oldMatchingDetails = PersonMatchRecord.from(it)
      context.personEntity?.update(context.person)
      val newMatchingDetails = PersonMatchRecord.from(it)
      context.personEntity = personRepository.save(it)
      context.hasMatchingFieldsChanged = oldMatchingDetails.matchingFieldsAreDifferent(newMatchingDetails)
    }
    return this
  }
}
