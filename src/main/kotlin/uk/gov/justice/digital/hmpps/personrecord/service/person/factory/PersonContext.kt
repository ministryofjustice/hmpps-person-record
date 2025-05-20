package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonContext(
  var personEntity: PersonEntity? = null,
)
