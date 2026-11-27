package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface PrisonPersonServiceProxy {
  fun processPersonUpdate(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity

  fun processPersonCreate(
    person: Person,
    findPerson: () -> PersonEntity?,
  )
}

@Profile("!prod & !preprod")
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
class PrisonPersonServiceProxyImpl(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {

  override fun processPersonCreate(
    person: Person,
    findPerson: () -> PersonEntity?,
  ) {
    // TODO once these changes have made it to production remove this method completely and stop listening to the search created event.
    // No op
  }

  override fun processPersonUpdate(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity {
    if (findPerson() == null) {
      throw ResourceNotFoundException("Person with ${person.prisonNumber} not found")
    }
    return personService.processPerson(
      person = person,
      childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
    ) {
      findPerson()
    }
  }
}

@Profile("prod | preprod")
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
class PrisonPersonServiceProxyProd(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {

  override fun processPersonCreate(
    person: Person,
    findPerson: () -> PersonEntity?,
  ) {
    processPersonUpdate(person, findPerson)
  }

  override fun processPersonUpdate(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = personService.processPerson(
    person = person,
    childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
  ) {
    findPerson()
  }
}
