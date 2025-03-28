package uk.gov.justice.digital.hmpps.personrecord.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Health.up
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */

@Component
class HealthInfo(buildProperties: BuildProperties, val matchClient: PersonMatchClient) : HealthIndicator {
  private val version: String = buildProperties.version

  override fun health(): Health {
    return try {
      val matchHealthStatus = matchClient.getHealth()
      val builder = Health.Builder().withDetail("version", version)
        .withDetail("PersonMatchStatus", Status(matchHealthStatus?.status))

      return when (matchHealthStatus?.status == Status.UP.toString()) {
        true -> builder.up().build()
        else -> builder.down().build()
      }
    } catch (e: Exception) {
      Health.down(e).withDetail("version", version).build()
    }
  }
}
