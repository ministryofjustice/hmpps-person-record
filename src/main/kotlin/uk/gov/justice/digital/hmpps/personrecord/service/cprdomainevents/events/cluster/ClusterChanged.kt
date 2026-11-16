package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class ClusterChanged(
  val personEntity: PersonEntity,
  val changeWasFromPersonDeleted: Boolean,
)
