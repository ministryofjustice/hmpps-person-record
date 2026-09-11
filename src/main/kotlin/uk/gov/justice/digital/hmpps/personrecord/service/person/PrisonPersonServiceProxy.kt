package uk.gov.justice.digital.hmpps.personrecord.service.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface PrisonPersonServiceProxy {
  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity
}
