package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface ChildBuilder {

  fun build(person: Person, personEntity: PersonEntity)

  fun <T, E> T.existsIn(childEntities: List<E>, match: (T, E) -> Boolean, yes: (E) -> E, no: () -> E?): E? {
    val found = childEntities.find { match(this, it) }
    return found?.let { yes(found) } ?: no()
  }
}
