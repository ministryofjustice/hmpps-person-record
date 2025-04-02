package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.net.URI

@Configuration
@Profile("test")
class LargeCourtMessageTestConfig {

  @Bean
  fun s3AsyncClient(@Value("\${aws.region}") awsRegion: String, @Value("\${aws.endpoint}") awsEndpoint: String): S3AsyncClient = S3AsyncClient.builder().region(
    Region.of(awsRegion),
  )
    .endpointOverride(URI.create(awsEndpoint))
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.builder().accessKeyId("any").secretAccessKey("any").build()),
    ).forcePathStyle(true).build()
}
