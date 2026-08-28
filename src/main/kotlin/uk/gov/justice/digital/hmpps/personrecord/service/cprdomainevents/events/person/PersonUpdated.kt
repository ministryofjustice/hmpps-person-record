package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker

data class PersonUpdated(
  val personEntity: PersonEntity,
  private val personChangeChecker: PersonChangeChecker,
) {
  fun personHasChanged(): Boolean = personChangeChecker.anyFieldsHaveChanged(personEntity)
  fun matchingFieldsHaveChanged(): Boolean = personChangeChecker.matchingFieldsHaveChanged(personEntity)
}
