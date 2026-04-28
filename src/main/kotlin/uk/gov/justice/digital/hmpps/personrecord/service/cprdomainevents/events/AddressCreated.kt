package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class AddressCreated(
  val crn: String,
  val addressId: String?,
  val addressEntity: AddressEntity,
)
