package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface PrisonPersonServiceProxy {
  fun processPersonUpdate(person: Person): PersonEntity

  fun processPersonCreate(person: Person)
}

@Profile("!prod & !preprod & !dev")
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
class PrisonPersonServiceProxyImpl(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {

  override fun processPersonCreate(person: Person) {
    // TODO turn this into a no-op.
    processPersonUpdate(person)
  }

  override fun processPersonUpdate(person: Person): PersonEntity = personService.processPerson(
    person = person,
    childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
  )
}

@Profile("prod | preprod | dev")
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
class PrisonPersonServiceProxyProd(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {

  override fun processPersonCreate(person: Person) {
    processPersonUpdate(person)
  }

  override fun processPersonUpdate(person: Person): PersonEntity = personService.processPerson(
    person = person,
    childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
  )
}
