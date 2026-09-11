package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface PrisonPersonServiceProxy {
  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity
}

@Component
@Profile("preprod | prod")
class PrisonPersonServiceProxyProd(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {
  override fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = personService.processPerson(person) { findPerson() }
}

@Component
@Profile("!preprod & !prod")
class PrisonPersonServiceProxyDev(
  private val personService: PersonService,
) : PrisonPersonServiceProxy {
  override fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = personService.processPerson(person, setOf(PseudonymEntity::class)) { findPerson() }
}
