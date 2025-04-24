package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class ClusterMerged(
  val from: PersonEntity,
  val to: PersonEntity,
  val fromMergedPersonKeyEntity: PersonKeyEntity,
)
