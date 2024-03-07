package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import uk.gov.justice.digital.hmpps.personrecord.client.model.PhoneNumber

@JsonIgnoreProperties(ignoreUnknown = true)
data class ContactDetails(
  val emailAddresses: List<String>? = emptyList(),
  val addresses: List<Address>? = emptyList(),
  val phoneNumbers: List<PhoneNumber>? = emptyList(),
) {
  fun getHomePhone(): String? {
    return phoneNumbers?.firstOrNull { it.type == "TELEPHONE" }?.number
  }

  fun getMobilePhone(): String? {
    return phoneNumbers?.firstOrNull { it.type == "MOBILE" }?.number
  }

  fun getEmail(): String? {
    return emailAddresses?.firstOrNull()
  }

  fun getAddress(): Address? {
    return addresses?.firstOrNull()
  }
}
