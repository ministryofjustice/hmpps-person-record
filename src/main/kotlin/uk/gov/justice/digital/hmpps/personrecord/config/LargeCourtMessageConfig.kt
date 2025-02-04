package uk.gov.justice.digital.hmpps.personrecord.config

import aws.sdk.kotlin.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test & !local")
class LargeCourtMessageConfig {

  @Bean
  fun s3Client(@Value("\${aws.region}") awsRegion: String): S3Client = S3Client {
    region = awsRegion
  }
}
