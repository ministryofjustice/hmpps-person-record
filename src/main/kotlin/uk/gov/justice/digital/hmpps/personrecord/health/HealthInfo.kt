package uk.gov.justice.digital.hmpps.personrecord.health

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component
class HealthInfo(personMatchWebClient: WebClient) : HealthPingCheck(personMatchWebClient)
