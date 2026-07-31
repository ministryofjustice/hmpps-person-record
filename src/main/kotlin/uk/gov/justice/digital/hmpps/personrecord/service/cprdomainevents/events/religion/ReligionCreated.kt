package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource

data class ReligionCreated(
  val domainEventSource: DomainEventSource,
  val prisonReligionEntity: PrisonReligionEntity,
)
