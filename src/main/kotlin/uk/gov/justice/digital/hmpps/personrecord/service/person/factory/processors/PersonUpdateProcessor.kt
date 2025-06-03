package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonUpdateProcessor(
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
) {

  fun updatePersonEntity(context: PersonContext): PersonUpdateProcessor {
    context.personEntity?.let {
      val oldMatchingDetails = PersonMatchRecord.from(it)
      context.personEntity?.update(context.person)
      personMatchService.saveToPersonMatch(context.personEntity!!)
      val newMatchingDetails = PersonMatchRecord.from(it)
      context.personEntity = personRepository.save(it)
      context.hasMatchingFieldsChanged = oldMatchingDetails.matchingFieldsAreDifferent(newMatchingDetails)
    }
    return this
  }
}
