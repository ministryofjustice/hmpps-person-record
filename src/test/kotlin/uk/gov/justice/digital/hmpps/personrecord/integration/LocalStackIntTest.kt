package uk.gov.justice.digital.hmpps.personrecord.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class LocalStackIntTest : IntegrationTestBase() {

  @Test
  fun `should send and receive message using local stack`() {
    // given
    val testTopic by lazy {
      hmppsQueueService.findByTopicId("testtopic")
    }
    val testQueue by lazy {
      hmppsQueueService.findByQueueId("testqueue")
    }

    val publishRequest = PublishRequest.builder()
      .topicArn(testTopic?.arn)
      .message("test message")
      .build()
    // when
    val publishResponse = testTopic?.snsClient?.publish(publishRequest)?.get()
    // then
    assertThat(publishResponse?.sdkHttpResponse()?.isSuccessful).isTrue()
    assertThat(publishResponse?.messageId()).isNotNull()

    await untilCallTo { testQueue?.sqsClient?.countMessagesOnQueue(testQueue!!.queueUrl)?.get() } matches { it == 1 }
  }
}
