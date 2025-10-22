package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class SelfHealed(
  val cluster: PersonKeyEntity,
)
