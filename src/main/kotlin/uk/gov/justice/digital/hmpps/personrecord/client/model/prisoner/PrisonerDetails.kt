package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonerDetails(
  val offenderNo: String,
  val offenderId: Long? = null,
  val rootOffenderId: Long? = null,
  val title: String? = null,
  val firstName: String? = null,
  val lastName: String? = null,
  val middleName: String? = null,
  val dateOfBirth: LocalDate,
  val birthPlace: String? = null,
  val birthCountryCode: String? = null,
  val physicalAttributes: PhysicalAttributes? = null,
  val identifiers: List<Identifier>? = emptyList(),
  var addresses: List<Address>? = emptyList(),
) {

  fun getPnc(): String? {
    return identifiers?.firstOrNull { it.type == "PNC" }?.value
  }

  fun getCro(): String? {
    return identifiers?.firstOrNull { it.type == "CRO" }?.value
  }

  fun getDrivingLicenseNumber(): String? {
    return identifiers?.firstOrNull { it.type == "DL" }?.value
  }

  fun getNationalInsuranceNumber(): String? {
    return identifiers?.firstOrNull { it.type == "NINO" }?.value
  }

  fun getHomeAddress(): Address? {
    return addresses?.firstOrNull { it.addressType == "HOME" }
  }
}
