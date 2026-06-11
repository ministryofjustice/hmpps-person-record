package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.SasGetAddressResponse

@Component
class SasClient(private val sasWebClient: WebClient) {

  fun getAddress(callBackUrl: String) = sasWebClient.get()
    .uri(callBackUrl)
    .retrieve()
    .bodyToMono(SasGetAddressResponse::class.java)
    .block()
}
