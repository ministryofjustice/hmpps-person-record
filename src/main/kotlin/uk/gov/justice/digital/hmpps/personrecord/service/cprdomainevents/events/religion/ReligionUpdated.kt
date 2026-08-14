package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource

data class ReligionUpdated(val domainEventSource: DomainEventSource, val prisonReligionEntity: PrisonReligionEntity, val sourceSystemType: SourceSystemType)
