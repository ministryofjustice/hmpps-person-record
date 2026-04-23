package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import java.util.UUID

data class PersonDeleted(
  val personEntity: PersonEntity,
  val cluster: PersonKeyEntity?,
  val uuidOfOverrideCluster: UUID? = null,
)
