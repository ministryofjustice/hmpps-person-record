package uk.gov.justice.digital.hmpps.personrecord.service.person.factories

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonChainable(
  val personEntity: PersonEntity,
  val matchingFieldsChanged: Boolean,
  val linkOnCreate: Boolean,
)
