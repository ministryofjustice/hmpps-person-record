package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
) {

  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = findPerson().exists(
    no = {
      personService.create(person)
    },
    yes = {
      personService.update(person, it)
    },
  )
}
