package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource

data class AddressDeleted(
  val addressEntity: AddressEntity,
  val personEntity: PersonEntity,
  val matchingFieldsHaveChanged: Boolean = false,
  val eventSource: DomainEventSource,
)
