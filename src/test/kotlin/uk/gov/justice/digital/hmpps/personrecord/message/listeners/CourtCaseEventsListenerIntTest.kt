package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithAdditionalFields
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendantAndNoPnc
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_EXACT_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_PARTIAL_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_CRO
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MATCH_CALL_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.SPLINK_MATCH_SCORE
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals

class CourtCaseEventsListenerIntTest : IntegrationTestBase() {

  @Test
  fun `should output correct telemetry for invalid CRO`() {
    val invalidCRO = "85227/65G" // G is the incorrect check letter
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(cro = invalidCRO), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      INVALID_CRO,
      mapOf("CRO" to invalidCRO),
    )
  }

  @Test
  fun `should successfully process common platform message with 3 defendants and create correct telemetry events`() {
    publishHMCTSMessage(commonPlatformHearing("19810154257C"), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to "1981/0154257C"),
    )
    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to "2008/0056560Z"),
    )
    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to ""),
    )
  }

  @Test
  fun `should not push messages from Common Platform onto dead letter queue when processing fails because of could not serialize access due to read write dependencies among transactions`() {
    // given
    val pncNumber = PNCIdentifier.from("2003/0062845E")

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
    val blitzer = Blitzer(100, 5)
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

    checkTelemetry(
      HMCTS_RECORD_CREATED,
      mapOf("PNC" to pncNumber.pncId),
    )
  }

  @Test
  fun `should create new defendant record from common platform message`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")

    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(), COMMON_PLATFORM_HEARING)

    val defendants = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(pncNumber)
    }

    assertThat(defendants.size).isEqualTo(1)
    assertThat(defendants[0].pncNumber).isEqualTo(pncNumber)
    assertThat(defendants[0].cro).isEqualTo(CROIdentifier.from("051072/62R"))
    assertThat(defendants[0].fingerprint).isEqualTo(true)
    assertThat(defendants[0].address).isNotNull
    assertThat(defendants[0].address?.addressLineOne).isEqualTo("13 broad Street")
    assertThat(defendants[0].address?.addressLineTwo).isEqualTo("Cardiff")
    assertThat(defendants[0].address?.addressLineThree).isEqualTo("Wales")
    assertThat(defendants[0].address?.addressLineFour).isEqualTo("UK")
    assertThat(defendants[0].address?.addressLineFive).isEqualTo("Earth")
    assertThat(defendants[0].address?.postcode).isEqualTo("CF10 1FU")
  }

  @Test
  fun `should create new defendants with additional fields from common platform message`() {
    val pncNumber1 = PNCIdentifier.from("2003/0062845E")
    val pncNumber2 = PNCIdentifier.from("2008/0056560Z")
    val pncNumber3 = PNCIdentifier.from("20230583843L")

    publishHMCTSMessage(commonPlatformHearingWithAdditionalFields(), COMMON_PLATFORM_HEARING)

    val defendantEntity1 = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(pncNumber1)
    }

    val defendantEntity2 = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(pncNumber2)
    }

    val defendantEntity3 = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(pncNumber3)
    }
    
    assertThat(defendantEntity1.size).isEqualTo(1)
    assertThat(defendantEntity1[0].pncNumber).isEqualTo(pncNumber1)
    assertThat(defendantEntity1[0].defendantId).isEqualTo("b5cfae34-9256-43ad-87fb-ac3def34e2ac")
    assertThat(defendantEntity1[0].masterDefendantId).isEqualTo("eeb71c73-573b-444e-9dc3-4e5998d1be65")
    assertThat(defendantEntity1[0].firstName).isEqualTo("Eric")
    assertThat(defendantEntity1[0].middleName).isEqualTo("mName1 mName2")
    assertThat(defendantEntity1[0].surname).isEqualTo("Lassard")
    assertThat(defendantEntity1[0].contact).isNull()
    assertThat(defendantEntity1[0].address).isNotNull()
    assertEquals(2, defendantEntity1[0].aliases.size)
    assertThat(defendantEntity1[0].aliases[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(defendantEntity1[0].aliases[0].surname).isEqualTo("alisLastName1")
    assertThat(defendantEntity1[0].aliases[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(defendantEntity1[0].aliases[1].surname).isEqualTo("alisLastName2")

    assertThat(defendantEntity2.size).isEqualTo(1)
    assertThat(defendantEntity2[0].aliases).isEmpty()
    assertThat(defendantEntity2[0].address).isNotNull()
    assertThat(defendantEntity2[0].pncNumber).isEqualTo(pncNumber2)
    assertThat(defendantEntity2[0].pncNumber).isEqualTo(pncNumber2)
    assertThat(defendantEntity2[0].contact?.homePhone).isEqualTo("0207345678")
    assertThat(defendantEntity2[0].contact?.workPhone).isEqualTo("0203788776")
    assertThat(defendantEntity2[0].contact?.mobile).isEqualTo("078590345677")
    assertThat(defendantEntity2[0].contact?.primaryEmail).isEqualTo("email@email.com")
    assertThat(defendantEntity2[0].defendantId).isEqualTo("cc36c035-6e82-4d04-94c2-2a5728f11481")
    assertThat(defendantEntity2[0].masterDefendantId).isEqualTo("1f6847a2-6663-44dd-b945-fe2c20961d0a")

    assertThat(defendantEntity3.size).isEqualTo(1)
    assertThat(defendantEntity3[0].aliases).isEmpty()
    assertThat(defendantEntity3[0].contact).isNull()
    assertThat(defendantEntity3[0].pncNumber).isEqualTo(pncNumber3)
    assertThat(defendantEntity3[0].nationalityCode).isNull()
    assertThat(defendantEntity3[0].sex).isNull()
    assertThat(defendantEntity3[0].nationalityOne).isNull()
    assertThat(defendantEntity3[0].nationalityTwo).isNull()
    assertThat(defendantEntity3[0].observedEthnicityDescription).isNull()
    assertThat(defendantEntity3[0].selfDefinedEthnicityDescription).isNull()
    assertThat(defendantEntity3[0].nationalInsuranceNumber).isEqualTo("PC456743D")
    assertThat(defendantEntity3[0].defendantId).isEqualTo("b56f8612-0f4c-43e5-840a-8bedb17722ec")
    assertThat(defendantEntity3[0].masterDefendantId).isEqualTo("290e0457-1480-4e62-b3c8-7f29ef791c58")
  }

  @Test
  fun `should output correct telemetry for exact match`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber.pncId), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      HMCTS_RECORD_CREATED,
      mapOf("PNC" to pncNumber.pncId),
    )

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber.pncId), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      HMCTS_EXACT_MATCH,
      mapOf("PNC" to pncNumber.pncId),
    )
  }

  @Test
  fun `should output correct telemetry and call person-match-score for partial match from common platform`() {
    val pncNumber = "2003/0062845E"

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Clancy", lastName = "Eccles", defendantId = "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777"), COMMON_PLATFORM_HEARING)
    checkTelemetry(
      HMCTS_RECORD_CREATED,
      mapOf("PNC" to "2003/0062845E"),
    )

    val defendants = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber))
    }
    assertThat(defendants.size).isEqualTo(1)

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Ken", lastName = "Eccles"), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      HMCTS_PARTIAL_MATCH,
      mapOf("Date of birth" to "1975-01-01"),
    )

    checkTelemetry(
      SPLINK_MATCH_SCORE,
      mapOf(
        "Match Probability Score" to "0.9897733",
        "Candidate Record Identifier Type" to "defendantId",
        "Candidate Record Identifier" to "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777",
        "New Record Identifier Type" to "defendantId",
        "New Record Identifier" to "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199",
      ),
    )
  }

  @Test
  fun `should output correct telemetry when call to person-match-score fails`() {
    val pncNumber = "2003/0062845E"

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Clancy", lastName = "Andy", defendantId = "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777"), COMMON_PLATFORM_HEARING)
    checkTelemetry(
      HMCTS_RECORD_CREATED,
      mapOf("PNC" to "2003/0062845E"),
    )

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Horace", lastName = "Andy"), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      MATCH_CALL_FAILED,
      emptyMap(),
    )
  }

  @Test
  fun `should output correct telemetry and call person-match-score for multiple partial matches, should send 1`() {
    val pncNumber = "2003/0062845E"

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Clancy", lastName = "Eccles", defendantId = "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777"), COMMON_PLATFORM_HEARING)
    val firstMatchEntity = await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber))
    }
    assertThat(firstMatchEntity.size).isEqualTo(1)

    val newDefendantEntity = DefendantEntity(pncNumber = PNCIdentifier.from(pncNumber), firstName = "John", surname = "Eccles", dateOfBirth = LocalDate.of(1975, 1, 1))
    defendantRepository.saveAndFlush(newDefendantEntity)

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Ken", lastName = "Eccles"), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      SPLINK_MATCH_SCORE,
      mapOf(
        "Match Probability Score" to "0.9897733",
        "Candidate Record Identifier Type" to "defendantId",
        "Candidate Record Identifier" to "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777",
        "New Record Identifier Type" to "defendantId",
        "New Record Identifier" to "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199",
      ),
    )
  }

  @Test
  fun `should output correct telemetry and call person-match-score for single partial match`() {
    val pncNumber = "2003/0062845E"

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "Clancy", lastName = "Eccles", defendantId = "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777"), COMMON_PLATFORM_HEARING)

    await.atMost(30, SECONDS) untilNotNull {
      defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber))
    }

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber, firstName = "John", lastName = "Eccles"), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      SPLINK_MATCH_SCORE,
      mapOf(
        "Match Probability Score" to "0.9897733",
        "Candidate Record Identifier Type" to "defendantId",
        "Candidate Record Identifier" to "9ff7c3e5-eb4c-4e3f-b9e6-b9e78d3ea777",
        "New Record Identifier Type" to "defendantId",
        "New Record Identifier" to "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199",
      ),
    )
  }

  @Test
  fun `should process messages without pnc`() {
    publishHMCTSMessage(commonPlatformHearingWithNewDefendantAndNoPnc(), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantsDefendantId("2d41e7b9-0964-48d8-8d2a-3f7e81b34cd7")
    }

    assertThat(personEntity.personId).isNotNull()

    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].pncNumber?.pncId).isEqualTo("")

    assertThat(personEntity.defendants).hasSize(1)
    assertThat(personEntity.defendants[0].pncNumber?.pncId).isEqualTo("")
  }
}
