package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface PrisonPersonServiceProxy {
  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity
}

@Component
class PrisonPersonServiceProxyImpl(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {
  override fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = personService.processPerson(
    person = person,
    childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
  ) {
    findPerson()
  }
}
