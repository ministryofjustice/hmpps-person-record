package uk.gov.justice.digital.hmpps.personrecord

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.personrecord.api.controller.probation.ProbationAPIController
import uk.gov.justice.digital.hmpps.personrecord.api.controller.probation.ProbationAddressCreateAPIController
import uk.gov.justice.digital.hmpps.personrecord.api.controller.probation.ProbationAddressGetAPIController
import uk.gov.justice.digital.hmpps.personrecord.config.SecurityConfiguration

@SpringBootConfiguration
@Import(
  ProbationAPIController::class,
  ProbationAddressCreateAPIController::class,
  ProbationAddressGetAPIController::class,
  SecurityConfiguration::class,
)
@EnableAutoConfiguration(
  excludeName = [
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
    "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "uk.gov.justice.hmpps.sqs.HmppsSqsConfiguration",
  ],
)
class PactTestConfiguration
