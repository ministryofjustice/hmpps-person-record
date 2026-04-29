package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sas.Address

@Component
class SasClient(private val sasWebClient: WebClient) {

  fun getAddress(addressId: String) = sasWebClient.get()
    .uri("/proposed-accommodations/{id}", addressId)
    .retrieve()
    .bodyToMono(Address::class.java)
    .block()
}
