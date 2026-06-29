package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated

interface PersonEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(personCreated: PersonCreated)
}

interface AddressEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(addressCreated: AddressCreated)
  fun onUpdate(addressUpdated: AddressUpdated)
  fun onDelete(addressDeleted: AddressDeleted)
}
