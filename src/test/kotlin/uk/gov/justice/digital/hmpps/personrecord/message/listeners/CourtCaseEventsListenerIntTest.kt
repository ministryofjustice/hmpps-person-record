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
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithAdditionalFields
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_CRO
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.concurrent.TimeUnit.SECONDS

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
      .message(commonPlatformHearingWithOneDefendant(pncNumber = pncNumber.pncId))
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
  fun `should create new person record from common platform message`() {
    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId("b5cfae34-9256-43ad-87fb-ac3def34e2ac")
    }

    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier.from("2003/0062845E"))
    assertThat(personEntity.cro).isEqualTo(CROIdentifier.from("51072/62R"))
    assertThat(personEntity.fingerprint).isEqualTo(true)
    assertThat(personEntity.addresses.size).isEqualTo(1)
    assertThat(personEntity.addresses[0].postcode).isEqualTo("CF10 1FU")
  }

  @Test
  fun `should update an existing person record from common platform message`() {
    val defendantId = "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199"
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(personEntity.lastName).isEqualTo("Andy")

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SourceSystem" to "HMCTS"),
    )

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(lastName = "Smith"), COMMON_PLATFORM_HEARING)

    val updatedPersonEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SourceSystem" to "HMCTS"),
    )

    assertThat(updatedPersonEntity.lastName).isEqualTo("Smith")
    assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier.from("1981/0154257C"))
    assertThat(updatedPersonEntity.cro).isEqualTo(CROIdentifier.from("86621/65B"))
  }

  @Test
  fun `should create new defendants with additional fields from common platform message`() {
    val pncNumber1 = PNCIdentifier.from("2003/0062845E")
    val pncNumber2 = PNCIdentifier.from("2008/0056560Z")
    val pncNumber3 = PNCIdentifier.from("20230583843L")

    publishHMCTSMessage(commonPlatformHearingWithAdditionalFields(), COMMON_PLATFORM_HEARING)

    val personEntity1 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId("b5cfae34-9256-43ad-87fb-ac3def34e2ac")
    }

    val personEntity2 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId("cc36c035-6e82-4d04-94c2-2a5728f11481")
    }

    val personEntity3 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId("b56f8612-0f4c-43e5-840a-8bedb17722ec")
    }

    assertThat(personEntity1.pnc).isEqualTo(pncNumber1)
    assertThat(personEntity1.defendantId).isEqualTo("b5cfae34-9256-43ad-87fb-ac3def34e2ac")
    assertThat(personEntity1.masterDefendantId).isEqualTo("eeb71c73-573b-444e-9dc3-4e5998d1be65")
    assertThat(personEntity1.firstName).isEqualTo("Eric")
    assertThat(personEntity1.middleNames).isEqualTo("mName1 mName2")
    assertThat(personEntity1.lastName).isEqualTo("Lassard")
    assertThat(personEntity1.contacts).isEmpty()
    assertThat(personEntity1.addresses).isNotEmpty()
    assertThat(personEntity1.aliases.size).isEqualTo(2)
    assertThat(personEntity1.aliases[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(personEntity1.aliases[0].lastname).isEqualTo("alisLastName1")
    assertThat(personEntity1.aliases[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(personEntity1.aliases[1].lastname).isEqualTo("alisLastName2")

    assertThat(personEntity2.aliases).isEmpty()
    assertThat(personEntity2.addresses).isNotEmpty()
    assertThat(personEntity2.addresses[0].postcode).isEqualTo("CF10 1FU")
    assertThat(personEntity2.pnc).isEqualTo(pncNumber2)
    assertThat(personEntity2.contacts.size).isEqualTo(3)
    assertThat(personEntity2.defendantId).isEqualTo("cc36c035-6e82-4d04-94c2-2a5728f11481")
    assertThat(personEntity2.masterDefendantId).isEqualTo("1f6847a2-6663-44dd-b945-fe2c20961d0a")

    assertThat(personEntity3.aliases).isEmpty()
    assertThat(personEntity3.contacts.size).isEqualTo(0)
    assertThat(personEntity3.pnc).isEqualTo(pncNumber3)
    assertThat(personEntity3.nationalInsuranceNumber).isEqualTo("PC456743D")
    assertThat(personEntity3.defendantId).isEqualTo("b56f8612-0f4c-43e5-840a-8bedb17722ec")
    assertThat(personEntity3.masterDefendantId).isEqualTo("290e0457-1480-4e62-b3c8-7f29ef791c58")
  }
}
