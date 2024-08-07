package uk.gov.justice.digital.hmpps.personrecord.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component
class PersonRecordHealthPing(

  webClient: WebClient,
  private val personMatchHealthPing: PersonMatchHealthPing,
) : HealthPingCheck(webClient) {

  override fun health(): Health {
    val personMatchHealth = personMatchHealthPing.health()

    return if (personMatchHealth.status == Status.UP) {
      super.health()
    } else {
      Health.down()
        .withDetail("PersonMatch", personMatchHealth.details)
        .build()
    }
  }
}
