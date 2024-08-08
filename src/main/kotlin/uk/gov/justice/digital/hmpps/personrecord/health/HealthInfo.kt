package uk.gov.justice.digital.hmpps.personrecord.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.MatchScoreClient

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */

@Component
class HealthInfo(buildProperties: BuildProperties, val matchScoreClient: MatchScoreClient) : HealthIndicator {
  private val version: String = buildProperties.version

  override fun health(): Health {
    return try {
      val healthStatus = matchScoreClient.getMatchHealth()
      return when (healthStatus?.status) {
        Status.UP.toString() -> Health.up().withDetail("version", version).withDetail("PersonMatchScoreStatus", Status.UP).build()
        else ->
          Health.down().withDetail("version", version).withDetail("PersonMatchScoreStatus", Status.DOWN).build()
      }
    } catch (e: Exception) {
      Health.down(e).withDetail("version", version).withDetail("PersonMatchScoreStatus", Status.DOWN).build()
    }
  }
}
