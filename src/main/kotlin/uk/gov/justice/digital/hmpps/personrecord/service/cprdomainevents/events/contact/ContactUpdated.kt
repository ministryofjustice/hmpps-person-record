package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource

data class ContactUpdated(
  val domainEventSource: DomainEventSource,
  val prisonNumber: String,
  val contactEntity: ContactEntity,
  val sourceSystemType: SourceSystemType,
)
