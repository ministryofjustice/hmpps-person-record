package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ContactDetails(
  val emailAddresses: List<String>? = emptyList(),
  val addresses: List<Address>? = emptyList(),
  val phoneNumbers: List<PhoneNumber>? =  emptyList(),
)
