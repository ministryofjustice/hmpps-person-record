package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class AddressUpdated(
  val addressEntity: AddressEntity,
  val matchingFieldsHaveChanged: Boolean = false,
)
