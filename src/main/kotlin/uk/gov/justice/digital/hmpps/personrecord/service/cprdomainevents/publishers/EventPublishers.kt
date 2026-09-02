package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionUpdated

interface PersonEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(personCreated: PersonCreated)
}

interface ReligionEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(religionCreated: ReligionCreated)
  fun onUpdate(religionUpdated: ReligionUpdated)
}

interface AddressEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(addressCreated: AddressCreated)
  fun onUpdate(addressUpdated: AddressUpdated)
  fun onDelete(addressDeleted: AddressDeleted)
}
