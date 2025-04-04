package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

@Configuration
@Profile("!test & !local")
class LargeCourtMessageConfig {

  @Bean
  fun s3AsyncClient(@Value("\${aws.region}") awsRegion: String): S3AsyncClient? = S3AsyncClient.builder().region(Region.of(awsRegion)).build()
}
