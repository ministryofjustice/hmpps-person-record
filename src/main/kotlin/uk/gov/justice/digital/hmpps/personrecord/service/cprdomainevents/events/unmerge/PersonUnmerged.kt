package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonUnmerged(
  val reactivatedRecord: PersonEntity,
  val unmergedRecord: PersonEntity,
)
