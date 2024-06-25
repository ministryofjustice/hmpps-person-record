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
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformMessageAlias
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
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
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.999999,
      ),
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
    val firstName = randomFirstName()
    val message = commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, firstName = firstName, pnc = pnc)))
    publishHMCTSMessage(message, COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(personEntity.firstName).isEqualTo(firstName)
    assertThat(personEntity.addresses.size).isEqualTo(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "HMCTS", "DEFENDANT_ID" to defendantId),
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf("0" to 0.999999),
    )
    stubMatchScore(matchResponse)

    val croIdentifier = randomCro()
    val messageId = publishHMCTSMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, firstName = firstName, pnc = pnc, cro = croIdentifier))),
      COMMON_PLATFORM_HEARING,
    )

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to HMCTS.name),
    )

    await.atMost(15, SECONDS) untilAsserted {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.firstName).isEqualTo(firstName)
      assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier.from(pnc))
      assertThat(updatedPersonEntity.cro).isEqualTo(CROIdentifier.from(croIdentifier))
      assertThat(updatedPersonEntity.fingerprint).isEqualTo(true)
      assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "HMCTS", "DEFENDANT_ID" to defendantId),
    )
  }

  @Test
  fun `should create new person with additional fields from common platform message`() {
    val pncNumber1 = randomPnc()

    val defendantId1 = randomUUID().toString()
    val niNumber = randomNationalInsuranceNumber()

    val defendantId1AliasFirstName = randomFirstName()
    val defendantId1AliasLastName = randomLastName()

    val messageId = publishHMCTSMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            defendantId = defendantId1,
            pnc = pncNumber1,
            nationalInsuranceNumber = niNumber,
            aliases = listOf(
              CommonPlatformMessageAlias(
                firstName = defendantId1AliasFirstName,
                lastName = defendantId1AliasLastName,
              ),
            ),
          ),
        ),
      ),
      COMMON_PLATFORM_HEARING,
    )

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to HMCTS.name),
    )

    val personEntity1 = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId1)
    }
    assertThat(personEntity1.pnc).isEqualTo(PNCIdentifier.from(pncNumber1))
    assertThat(personEntity1.nationalInsuranceNumber).isEqualTo(niNumber)
    assertThat(personEntity1.aliases.size).isEqualTo(1)
    assertThat(personEntity1.aliases[0].firstName).isEqualTo(defendantId1AliasFirstName)
    assertThat(personEntity1.aliases[0].lastName).isEqualTo(defendantId1AliasLastName)
  }

  @Test
  fun `should process messages with pnc as empty string and null`() {
    val id1 = randomUUID().toString()
    val id2 = randomUUID().toString()

    val messageId = publishHMCTSMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = id1, pnc = null),
          CommonPlatformHearingSetup(defendantId = id2, pnc = ""),
        ),
      ),
      COMMON_PLATFORM_HEARING,
    )

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to HMCTS.name),
    )

    val personEntity = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id1)
    }
    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    val secondPersonEntity = personRepository.findByDefendantId(id2)
    assertThat(secondPersonEntity?.pnc?.pncId).isEqualTo("")
  }

  private fun buildPublishRequest(
    defendantId: String,
    pncNumber: PNCIdentifier,
  ): PublishRequest? = PublishRequest.builder()
    .topicArn(courtCaseEventsTopic?.arn)
    .message(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, pnc = pncNumber.pncId),
          CommonPlatformHearingSetup(defendantId = defendantId, pnc = pncNumber.pncId),
        ),
      ),
    )
    .messageAttributes(
      mapOf(
        "messageType" to MessageAttributeValue.builder().dataType("String")
          .stringValue(COMMON_PLATFORM_HEARING.name).build(),
      ),
    )
    .build()
}
