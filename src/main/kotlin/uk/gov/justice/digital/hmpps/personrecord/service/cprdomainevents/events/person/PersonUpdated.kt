package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker

data class PersonUpdated(
  val personEntity: PersonEntity,
  val personChangeChecker: PersonChangeChecker,
) {
  fun matchingFieldsHaveChanged(): Boolean = personChangeChecker.matchingFieldsHaveChanged(personEntity)
}
