package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@Sql(
  scripts = ["classpath:sql/before-test.sql"],
  config = SqlConfig(transactionMode = ISOLATED),
  executionPhase = BEFORE_TEST_METHOD,
)
@Sql(
  scripts = ["classpath:sql/after-test.sql"],
  config = SqlConfig(transactionMode = ISOLATED),
  executionPhase = AFTER_TEST_METHOD,
)
@Suppress("INLINE_FROM_HIGHER_PLATFORM")
class CourtCaseEventsListenerIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `should output correct telemetry for invalid PNC`() {
    // given
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearing("03/62845X")) // X is the incorrect check letter
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()

    // when
    courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(INVALID_PNC),
        check {
          assertThat(it["PNC"]).isEqualTo("2003/0062845X").withFailMessage("PNC incorrect")
          assertThat(it["inputPNC"]).isEqualTo("03/62845X").withFailMessage("inputPNC incorrect")
        },
      )
    }
  }

  @Test
  fun `should successfully process common platform message and create correct telemetry events`() {
    // given
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearing("19810154257C"))
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()

    // when
    courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_CP_CASE_RECEIVED),
        check {
          assertThat(it["PNC"]).isEqualTo("1981/0154257C")
        },
      )
    }
  }

  @Test
  fun `should process libra messages with empty pnc identifier`() {
    // given
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(libraHearing(pncNumber = ""))
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(MessageType.LIBRA_COURT_CASE.name).build(),
        ),
      )
      .build()

    // when
    courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.MISSING_PNC),
        check {
          assertThat(it).isEmpty()
        },
      )
    }

    verify(telemetryService, never()).trackEvent(
      eq(INVALID_PNC),
      check {
        assertThat(it["PNC"]).isEqualTo("")
      },
    )
  }

  @Test
  fun `should successfully process libra message from court_case_events_topic`() {
    // given
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(libraHearing("19231234567A"))
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(MessageType.LIBRA_COURT_CASE.name).build(),
        ),
      )
      .build()

    // when
    courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(TelemetryEventType.NEW_LIBRA_CASE_RECEIVED),
        check {
          assertThat(it["PNC"]).isEqualTo("1923/1234567A")
          assertThat(it["CRO"]).isEqualTo("11111/79J")
        },
      )
    }
  }

  @Test
  fun `should not push messages from Libra onto dead letter queue when processing fails because of could not serialize access due to read write dependencies among transactions`() {
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
    val blitzer = Blitzer(100, 50)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsDlqClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }

    verify(telemetryService, times(1)).trackEvent(
      eq(TelemetryEventType.NEW_CASE_PERSON_CREATED),
      check {
        assertThat(it["PNC"]).isEqualTo("2003/0011985X")
      },
    )
  }

  @Test
  fun `should not push messages from Common Platform onto dead letter queue when processing fails because of could not serialize access due to read write dependencies among transactions`() {
    // given
    val pncNumber = PNCIdentifier("2003/0062845E")

    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearingWithNewDefendant())
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()
    // when
    val blitzer = Blitzer(100, 50)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsDlqClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }

    await untilAsserted { assertThat(postgresSQLContainer.isCreated).isTrue() }

    val personEntity = await.atMost(30, TimeUnit.SECONDS) untilNotNull {
      personRepository.findByPrisonersPncNumber(pncNumber)
    }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.defendants.size).isEqualTo(1)
    assertThat(personEntity.defendants[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].crn).isEqualTo("X026350")
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.offenders[0].firstName).isEqualTo("Eric")
    assertThat(personEntity.offenders[0].lastName).isEqualTo("Lassard")
    assertThat(personEntity.offenders[0].dateOfBirth).isEqualTo(LocalDate.of(1960, 1, 1))
    assertThat(personEntity.prisoners).hasSize(1)
    assertThat(personEntity.prisoners[0].prisonNumber).isEqualTo("A1234AA")
    assertThat(personEntity.prisoners[0].pncNumber).isEqualTo(pncNumber)

    verify(telemetryService, times(1)).trackEvent(
      eq(TelemetryEventType.NEW_CASE_PERSON_CREATED),
      check {
        assertThat(it["PNC"]).isEqualTo(pncNumber.pncId)
      },
    )
  }

  @Test
  fun `should create new defendant and prisoner records with link to a person record from common platform message`() {
    // given
    val pncNumber = PNCIdentifier("2003/0062845E")

    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearingWithNewDefendant())
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()

    // when
    courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()

    // then
    await untilCallTo {
      cprCourtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(cprCourtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilAsserted { assertThat(postgresSQLContainer.isCreated).isTrue() }

    val personEntity = await.atMost(30, TimeUnit.SECONDS) untilNotNull {
      personRepository.findByPrisonersPncNumber(pncNumber)
    }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.defendants.size).isEqualTo(1)
    assertThat(personEntity.defendants[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].crn).isEqualTo("X026350")
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.offenders[0].firstName).isEqualTo("Eric")
    assertThat(personEntity.offenders[0].lastName).isEqualTo("Lassard")
    assertThat(personEntity.offenders[0].dateOfBirth).isEqualTo(LocalDate.of(1960, 1, 1))
    assertThat(personEntity.offenders[0].prisonNumber).isEqualTo("A1671AJ")
    assertThat(personEntity.prisoners).hasSize(1)
    assertThat(personEntity.prisoners[0].firstName).isEqualTo("ERIC")
    assertThat(personEntity.prisoners[0].lastName).isEqualTo("Lassard")
    assertThat(personEntity.prisoners[0].prisonNumber).isEqualTo("A1234AA")
    assertThat(personEntity.prisoners[0].pncNumber).isEqualTo(pncNumber)
  }
}
