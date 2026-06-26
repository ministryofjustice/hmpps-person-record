package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.updater

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import kotlin.reflect.KClass

interface PersonUpdater {
  fun update(
    person: Person,
    childrenToIgnore: Set<KClass<*>>,
    personEntity: PersonEntity,
  )
}
