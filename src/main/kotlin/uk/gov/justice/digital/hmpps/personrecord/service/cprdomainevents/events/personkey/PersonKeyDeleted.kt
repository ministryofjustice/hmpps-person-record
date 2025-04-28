package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class PersonKeyDeleted(
  val personEntity: PersonEntity,
  val personKeyEntity: PersonKeyEntity,
)
