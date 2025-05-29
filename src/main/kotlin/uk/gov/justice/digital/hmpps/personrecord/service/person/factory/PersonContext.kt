package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

enum class PersonOperation {
  CREATE,
  UPDATE,
}

data class PersonContext(
  val person: Person,
  var personEntity: PersonEntity? = null,
  var operation: PersonOperation? = null,
)
