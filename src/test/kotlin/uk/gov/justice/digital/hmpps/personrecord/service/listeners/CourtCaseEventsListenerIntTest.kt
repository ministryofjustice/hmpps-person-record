package uk.gov.justice.digital.hmpps.personrecord.service.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.service.helper.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@Sql(
  scripts = ["classpath:sql/before-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
  scripts = ["classpath:sql/after-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
@Suppress("INLINE_FROM_HIGHER_PLATFORM")
class CourtCaseEventsListenerIntTest : IntegrationTestBase() {

  val courtCaseEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcaseeventstopic")
  }
  val cprCourtCaseEventsQueue by lazy {
    hmppsQueueService.findByQueueId("cprcourtcaseeventsqueue")
  }

  @Test
  fun `should successfully process common platform message and create correct telemetry events`() {
    // given
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearing())
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(MessageType.COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()

    // when
    val publishResponse = courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    assertThat(publishResponse?.sdkHttpResponse()?.isSuccessful).isTrue()
    assertThat(publishResponse?.messageId()).isNotNull()

    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_CASE_EXACT_MATCH),
        check {
          assertThat(it["PNC"]).isEqualTo("1981/0154257C")
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_CASE_PARTIAL_MATCH),
        check {
          assertThat(it["Surname"]).isEqualTo("Potter")
          assertThat(it["Forename"]).isEqualTo("Harry")
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_CASE_MISSING_PNC),
        check {
          assertThat(it).isEmpty()
        },
      )
    }
  }

  @Test
  fun `should successfully process libra message from court_case_events_topic`() {
    // given
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(libraHearing())
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(MessageType.LIBRA_COURT_CASE.name).build(),
        ),
      )
      .build()

    // when
    val publishResponse = courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    assertThat(publishResponse?.sdkHttpResponse()?.isSuccessful).isTrue()
    assertThat(publishResponse?.messageId()).isNotNull()

    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_CASE_INVALID_PNC),
        check {
          assertThat(it["PNC"]).isEqualTo("1923[1234567A")
        },
      )
    }
  }
}
