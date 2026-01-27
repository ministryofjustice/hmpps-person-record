package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Profile("!preprod & !prod")
@Component
class ServiceNowClient(private val serviceNowWebClient: WebClient) {
  fun postRecords(payload: ServiceNowPayload): ServiceNowResponse = serviceNowWebClient
    .post()
    .uri("/api/sn_sc/servicecatalog/items/order_now")
    .bodyValue(payload)
    .retrieve()
    .bodyToMono(ServiceNowResponse::class.java)
    .block()!!
}
