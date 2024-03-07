package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.client.model.PhoneNumber

@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  @JsonProperty("postalCode")
  val postCode: String? = null,
  val addressType: String? = null,
  val phones: List<PhoneNumber>? = emptyList(),
) {

  fun getHomePhone(): String? {
    return phones?.firstOrNull { it.type == "HOME" }?.number
  }

  fun getMobilePhone(): String? {
    return phones?.firstOrNull { it.type == "MOB" }?.number
  }
}
