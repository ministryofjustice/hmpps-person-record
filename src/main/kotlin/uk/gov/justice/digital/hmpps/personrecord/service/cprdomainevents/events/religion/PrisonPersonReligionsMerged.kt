package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PrisonPersonReligionsMerged(
  val from: PersonEntity?,
  val to: PersonEntity,
)
