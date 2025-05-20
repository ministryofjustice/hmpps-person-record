package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.mixin

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext

interface CreateMixin {
  val context: PersonContext
  val personRepository: PersonRepository

  fun create(person: Person): CreateMixin {
    val personEntity = PersonEntity.new(person)
    context.personEntity = personRepository.save(personEntity)
    return this
  }
}