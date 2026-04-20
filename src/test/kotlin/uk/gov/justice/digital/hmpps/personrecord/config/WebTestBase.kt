package uk.gov.justice.digital.hmpps.personrecord.config

import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.UUID
import java.util.concurrent.CompletableFuture

@TestPropertySource(properties = ["spring.autoconfigure.exclude=uk.gov.justice.hmpps.sqs.HmppsSqsConfiguration"])
@Import(WebTestBase.WebTestConfig::class)
abstract class WebTestBase : WebTestSetup() {

  @TestConfiguration
  class WebTestConfig {
    @Bean
    fun hmppsQueueService(): HmppsQueueService {
      val hmppsQueueService = Mockito.mock(HmppsQueueService::class.java)
      val mockClient = Mockito.mock(SnsAsyncClient::class.java)
      val response = PublishResponse.builder().messageId(UUID.randomUUID().toString()).build()

      whenever(mockClient.publish(any<PublishRequest>()))
        .thenReturn(CompletableFuture.completedFuture(response))

      whenever(hmppsQueueService.findByTopicId("cprcourtcasestopic")).thenReturn(
        HmppsTopic(
          id = "mock",
          arn = "mock",
          mockClient,
        ),
      )

      whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(
        HmppsTopic(
          id = "mock",
          arn = "mock",
          mockClient,
        ),
      )

      return hmppsQueueService
    }
  }
}
