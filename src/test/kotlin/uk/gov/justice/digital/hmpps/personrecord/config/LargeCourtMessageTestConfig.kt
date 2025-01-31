package uk.gov.justice.digital.hmpps.personrecord.config

import aws.sdk.kotlin.services.s3.S3Client
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("test")
class LargeCourtMessageTestConfig { // (@Value("\${aws.region-name}") private val regionName: String,)

  @Bean
  fun s3Client(): S3Client = S3Client
    .builder()
    .build()
}
