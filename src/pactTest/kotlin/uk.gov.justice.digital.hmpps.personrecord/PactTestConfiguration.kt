package uk.gov.justice.digital.hmpps.personrecord

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.personrecord.api.controller.probation.ProbationAddressGetAPIController
import uk.gov.justice.digital.hmpps.personrecord.config.SecurityConfiguration

@TestConfiguration
//@EnableWebMvc
@Import(
  ProbationAddressGetAPIController::class,
  SecurityConfiguration::class,
)
@SpringBootApplication(
  excludeName = [
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
    "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
    "uk.gov.justice.hmpps.sqs.HmppsSqsConfiguration",
  ]
)
class PactTestConfiguration