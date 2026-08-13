package uk.gov.justice.digital.hmpps.personrecord

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@SpringBootApplication
class HmppsPersonRecord

fun main(args: Array<String>) {
  runApplication<HmppsPersonRecord>(*args)
}
