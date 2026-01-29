package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Profile("!preprod & !prod")
@Component
class ServiceNowMergeRequestClient(private val serviceNowWebClient: WebClient, @Value($$"${service-now.sysparm-id}")private val sysParmId: String) {
  fun postRecords(payload: ServiceNowPayload) = serviceNowWebClient
    .post()
    .uri("/api/sn_sc/servicecatalog/items/$sysParmId/order_now")
    .bodyValue(payload)
    .retrieve()
    .toBodilessEntity()
    .block()!!
}
