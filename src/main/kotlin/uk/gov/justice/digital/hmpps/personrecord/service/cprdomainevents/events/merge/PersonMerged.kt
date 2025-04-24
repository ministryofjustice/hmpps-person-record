package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonMerged(
  val from: PersonEntity?,
  val to: PersonEntity,
)
