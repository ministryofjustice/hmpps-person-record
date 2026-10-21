package uk.gov.justice.digital.hmpps.personrecord.service.message

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

interface PersonGranularDataMerger {
  fun handleMerge(fromPerson: PersonEntity, toPerson: PersonEntity, person: Person)
}
