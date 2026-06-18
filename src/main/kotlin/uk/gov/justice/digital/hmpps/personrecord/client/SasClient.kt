package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address

@Component
class SasClient(private val sasWebClient: WebClient) {

  fun getAddress(callBackUrl: String): SasAddress {
    val addressData = sasWebClient.get()
      .uri(callBackUrl)
      .retrieve()
      .bodyToMono<SasGetAddressResponse>()
      .block()!!.data

    return SasAddress(
      Address.from(addressData),
      addressData.crn,
      addressData.cprAddressId,
    )
  }
}

data class SasAddress(
  val address: Address,
  val crn: String,
  val cprAddressId: String,
)
