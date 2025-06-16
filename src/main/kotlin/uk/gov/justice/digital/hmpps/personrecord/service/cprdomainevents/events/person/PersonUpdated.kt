package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonUpdated(
  val personEntity: PersonEntity,
  val matchingFieldsHaveChanged: Boolean = false,
)
