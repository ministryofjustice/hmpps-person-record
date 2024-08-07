package uk.gov.justice.digital.hmpps.personrecord.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component

/**
 * Adds version data to the /health endpoint. This is called by the UI to display API details
 */

@Component
class HealthInfo(buildProperties: BuildProperties) : HealthIndicator {
  private val version: String = buildProperties.version

  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

  override fun health(): Health {
    val personRecordHealth = personRecordHealthPing.health()
    return if (personRecordHealth.status == Status.UP) {
      Health.up().withDetail("version", version).build()
    } else {
      Health.down()
        .withDetail("version", version)
        .withDetail("PersonRecordApi", personRecordHealth.details)
        .build()
    }
  }
}
