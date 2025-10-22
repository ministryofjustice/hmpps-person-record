package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class ReviewRemoved(
  val primaryCluster: PersonKeyEntity,
)
