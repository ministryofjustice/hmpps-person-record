package uk.gov.justice.digital.hmpps.personrecord

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@SpringBootApplication
@EnableJpaAuditing
class HmppsPersonRecord

fun main(args: Array<String>) {
  runApplication<HmppsPersonRecord>(*args)
}
