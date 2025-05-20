package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.mixin

import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext

interface UpdateMixin {
  val context: PersonContext
  val personRepository: PersonRepository

  fun update(person: Person): UpdateMixin {
    context.personEntity?.let {
      it.update(person)
      context.personEntity = personRepository.save(it)
    }
    return this
  }
}