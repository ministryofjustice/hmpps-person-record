package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.HMCTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.COURT_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearingWithAdditionalFields
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearingWithNewDefendantAndNoPnc
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearingWithSameDefendantIdTwice
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit.SECONDS

class CommonPlatformCourtCaseListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should successfully process common platform message with 3 defendants and create correct telemetry events`() {
    val firstDefendantId = randomUUID().toString()
    val secondDefendantId = randomUUID().toString()
    val thirdDefendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val secondPnc = randomPnc()
    val messageId = publishHMCTSMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = firstDefendantId, pnc = firstPnc), CommonPlatformHearingSetup(defendantId = secondDefendantId, pnc = secondPnc), CommonPlatformHearingSetup(defendantId = thirdDefendantId, pnc = ""))), COMMON_PLATFORM_HEARING)

    await.atMost(10, SECONDS) untilNotNull {
      assertThat(personRepository.findByDefendantId(firstDefendantId))
    }

    await.atMost(10, SECONDS) untilNotNull {
      assertThat(personRepository.findByDefendantId(secondDefendantId))
    }

    await.atMost(10, SECONDS) untilNotNull {
      assertThat(personRepository.findByDefendantId(thirdDefendantId))
    }

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "PNC" to firstPnc,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to HMCTS.name,
      ),
    )
    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "PNC" to secondPnc,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to HMCTS.name,
      ),
    )
    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf(
        "PNC" to "",
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to HMCTS.name,
      ),
    )
  }

  @Test
  fun `should not push messages from Common Platform onto dead letter queue when processing fails - fires the same request so many times that some message writes will fail and be retried`() {
    val pncNumber = PNCIdentifier.from(randomPnc())
    val defendantId = randomUUID().toString()

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf("0" to 0.999999),
    )
    stubMatchScore(matchResponse)

    val blitzer = Blitzer(15, 4)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(buildPublishRequest(defendantId, pncNumber))?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    await untilCallTo {
      courtCaseEventsQueue?.sqsClient?.countMessagesOnQueue(courtCaseEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilCallTo {
      courtCaseEventsQueue?.sqsDlqClient?.countMessagesOnQueue(courtCaseEventsQueue!!.dlqUrl!!)?.get()
    } matches { it == 0 }

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "HMCTS", "DEFENDANT_ID" to defendantId),
    )
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf(
        "SOURCE_SYSTEM" to "HMCTS",
        "DEFENDANT_ID" to defendantId,
      ),
      29,
    )
  }

  @Test
  fun `should update an existing person record from common platform message`() {
    val defendantId = randomUUID().toString()
    val pnc = randomPnc()
    val message = commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId)))
    publishHMCTSMessage(message, COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(personEntity.lastName).isEqualTo("Andy")
    assertThat(personEntity.addresses.size).isEqualTo(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "HMCTS", "DEFENDANT_ID" to defendantId),
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf("0" to 0.999999),
    )
    stubMatchScore(matchResponse)
    val messageId = publishHMCTSMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, lastName = "Smith", pnc = pnc))),
      COMMON_PLATFORM_HEARING,
    )

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to HMCTS.name),
    )

    await.atMost(15, SECONDS) untilAsserted {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.lastName).isEqualTo("Smith")
      assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier.from(pnc))
      assertThat(updatedPersonEntity.cro).isEqualTo(CROIdentifier.from("86621/65B"))
      assertThat(updatedPersonEntity.fingerprint).isEqualTo(true)
      assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "HMCTS", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should create new people with additional fields from common platform message`() {
    val pncNumber1 = PNCIdentifier.from("2003/0062845E")
    val pncNumber2 = PNCIdentifier.from("2008/0056560Z")
    val pncNumber3 = PNCIdentifier.from("20230583843L")

    val id1 = randomUUID().toString()
    val id2 = randomUUID().toString()
    val id3 = randomUUID().toString()

    publishHMCTSMessage(commonPlatformHearingWithAdditionalFields(defendantIds = listOf(id1, id2, id3)), COMMON_PLATFORM_HEARING)

    val personEntity1 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id1)
    }

    val personEntity2 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id2)
    }

    val personEntity3 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id3)
    }

    assertThat(personEntity1.pnc).isEqualTo(pncNumber1)
    assertThat(personEntity1.masterDefendantId).isEqualTo("eeb71c73-573b-444e-9dc3-4e5998d1be65")
    assertThat(personEntity1.firstName).isEqualTo("Eric")
    assertThat(personEntity1.middleNames).isEqualTo("mName1 mName2")
    assertThat(personEntity1.lastName).isEqualTo("Lassard")
    assertThat(personEntity1.contacts).isEmpty()
    assertThat(personEntity1.addresses).isNotEmpty()
    assertThat(personEntity1.aliases.size).isEqualTo(2)
    assertThat(personEntity1.aliases[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(personEntity1.aliases[0].lastName).isEqualTo("alisLastName1")
    assertThat(personEntity1.aliases[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(personEntity1.aliases[1].lastName).isEqualTo("alisLastName2")

    assertThat(personEntity2.aliases).isEmpty()
    assertThat(personEntity2.addresses).isNotEmpty()
    assertThat(personEntity2.addresses[0].postcode).isEqualTo("CF10 1FU")
    assertThat(personEntity2.pnc).isEqualTo(pncNumber2)
    assertThat(personEntity2.contacts.size).isEqualTo(3)
    assertThat(personEntity2.masterDefendantId).isEqualTo("1f6847a2-6663-44dd-b945-fe2c20961d0a")

    assertThat(personEntity3.aliases).isEmpty()
    assertThat(personEntity3.contacts.size).isEqualTo(0)
    assertThat(personEntity3.pnc).isEqualTo(pncNumber3)
    assertThat(personEntity3.nationalInsuranceNumber).isEqualTo("PC456743D")
    assertThat(personEntity3.masterDefendantId).isEqualTo("290e0457-1480-4e62-b3c8-7f29ef791c58")
  }

  @Test
  fun `should process messages with pnc as empty string and null`() {
    val id1 = randomUUID().toString()
    val id2 = randomUUID().toString()
    publishHMCTSMessage(commonPlatformHearingWithNewDefendantAndNoPnc(defendantIds = listOf(id1, id2)), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id1)
    }
    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    val secondPersonEntity = personRepository.findByDefendantId(id2)
    assertThat(secondPersonEntity?.pnc?.pncId).isEqualTo("")
    assertThat(secondPersonEntity?.cro?.croId).isEqualTo("075715/64Q")
  }

  private fun buildPublishRequest(
    defendantId: String,
    pncNumber: PNCIdentifier,
  ): PublishRequest? = PublishRequest.builder()
    .topicArn(courtCaseEventsTopic?.arn)
    .message(commonPlatformHearingWithSameDefendantIdTwice(defendantId = defendantId, pncNumber = pncNumber.pncId))
    .messageAttributes(
      mapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(COMMON_PLATFORM_HEARING.name).build(),
      ),
    )
    .build()
}
