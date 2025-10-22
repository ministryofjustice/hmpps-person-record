package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class OverrideConflict(
  val cluster: PersonKeyEntity,
  val additionalClusters: List<PersonKeyEntity>,
)
