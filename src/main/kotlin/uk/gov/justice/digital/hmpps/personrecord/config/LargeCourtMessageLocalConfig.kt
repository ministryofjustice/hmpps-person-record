package uk.gov.justice.digital.hmpps.personrecord.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
class LargeCourtMessageLocalConfig {

  @Bean
  fun s3Client(@Value("\${aws.region}") awsRegion: String, @Value("\${aws.endpoint}") awsEndpoint: String): S3Client = S3Client {
    region = awsRegion
    endpointUrl = Url.parse(awsEndpoint)
    credentialsProvider = StaticCredentialsProvider {
      accessKeyId = "any"
      secretAccessKey = "any"
    }
    forcePathStyle = true
  }
}
