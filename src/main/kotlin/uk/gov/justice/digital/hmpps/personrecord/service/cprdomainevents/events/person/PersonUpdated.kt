package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonMatchChecker

data class PersonUpdated(
  val personEntity: PersonEntity,
  private val personMatchChecker: PersonMatchChecker,
) {

  fun personHasChanged(): Boolean = personMatchChecker.isDifferentFrom(personEntity)

  fun matchingFieldsHaveChanged(): Boolean = personMatchChecker.matchingFieldsAreDifferent(personEntity)
}
