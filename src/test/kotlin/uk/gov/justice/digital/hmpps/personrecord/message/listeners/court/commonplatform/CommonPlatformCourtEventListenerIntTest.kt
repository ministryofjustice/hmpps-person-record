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
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.HMCTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.COURT_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupContact
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit.SECONDS

class CommonPlatformCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should successfully process common platform message with 3 defendants and create correct telemetry events`() {
    val firstDefendantId = randomUUID().toString()
    val secondDefendantId = randomUUID().toString()
    val thirdDefendantId = randomUUID().toString()
    val firstPnc = randomPnc()
    val secondPnc = randomPnc()
    val messageId = publishCourtMessage(commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = firstDefendantId, pnc = firstPnc), CommonPlatformHearingSetup(defendantId = secondDefendantId, pnc = secondPnc), CommonPlatformHearingSetup(defendantId = thirdDefendantId, pnc = ""))), COMMON_PLATFORM_HEARING)

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
        courtEventsTopic?.snsClient?.publish(buildPublishRequest(defendantId, pncNumber))?.get()
      }
    } finally {
      blitzer.shutdown()
    }

    await untilCallTo {
      courtEventsQueue?.sqsClient?.countMessagesOnQueue(courtEventsQueue!!.queueUrl)?.get()
    } matches { it == 0 }

    await untilCallTo {
      courtEventsQueue?.sqsDlqClient?.countMessagesOnQueue(courtEventsQueue!!.dlqUrl!!)?.get()
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
    val cro = randomCro()
    val firstName = randomFirstName()
    val lastName = randomLastName()
    val message = commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, firstName = firstName, lastName = lastName, pnc = pnc, cro = cro)))
    publishCourtMessage(message, COMMON_PLATFORM_HEARING)

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

    val changedLastName = randomLastName()
    val messageId = publishCourtMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, lastName = changedLastName, pnc = pnc, cro = cro))),
      COMMON_PLATFORM_HEARING,
    )

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to HMCTS.name),
    )

    await.atMost(15, SECONDS) untilAsserted {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier.from(pnc))
      assertThat(updatedPersonEntity.cro).isEqualTo(CROIdentifier.from(cro))
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
    val firstPnc = randomPnc()
    val firstName = randomFirstName()
    val lastName = randomLastName()
    val secondPnc = randomPnc()
    val thirdPnc = randomPnc()

    val firstDefendantId = randomUUID().toString()
    val secondDefendantId = randomUUID().toString()
    val thirdDefendantId = randomUUID().toString()

    val thirdDefendantNINumber = randomNationalInsuranceNumber()

    publishCourtMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            defendantId = firstDefendantId,
            pnc = firstPnc,
            firstName = firstName,
            middleName = "mName1 mName2",
            lastName = lastName,
            aliases = listOf(
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName1", lastName = "alisLastName1"),
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName2", lastName = "alisLastName2"),
            ),
          ),
          CommonPlatformHearingSetup(defendantId = secondDefendantId, pnc = secondPnc, contact = CommonPlatformHearingSetupContact()),
          CommonPlatformHearingSetup(defendantId = thirdDefendantId, pnc = thirdPnc, nationalInsuranceNumber = thirdDefendantNINumber),
        ),
      ),
      COMMON_PLATFORM_HEARING,
    )

    val firstPerson = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(firstDefendantId)
    }

    val secondPerson = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(secondDefendantId)
    }

    val thirdPerson = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(thirdDefendantId)
    }

    assertThat(firstPerson.pnc).isEqualTo(PNCIdentifier.from(firstPnc))
    assertThat(firstPerson.personIdentifier).isNull()
    assertThat(firstPerson.masterDefendantId).isEqualTo(firstDefendantId)
    assertThat(firstPerson.firstName).isEqualTo(firstName)
    assertThat(firstPerson.middleNames).isEqualTo("mName1 mName2")
    assertThat(firstPerson.lastName).isEqualTo(lastName)
    assertThat(firstPerson.contacts).isEmpty()
    assertThat(firstPerson.addresses).isNotEmpty()
    assertThat(firstPerson.aliases.size).isEqualTo(2)
    assertThat(firstPerson.aliases[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(firstPerson.aliases[0].lastName).isEqualTo("alisLastName1")
    assertThat(firstPerson.aliases[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(firstPerson.aliases[1].lastName).isEqualTo("alisLastName2")

    assertThat(secondPerson.aliases).isEmpty()
    assertThat(secondPerson.addresses).isNotEmpty()
    assertThat(secondPerson.addresses[0].postcode).isEqualTo("CF10 1FU")
    assertThat(secondPerson.pnc).isEqualTo(PNCIdentifier.from(secondPnc))
    assertThat(secondPerson.contacts.size).isEqualTo(3)
    assertThat(secondPerson.masterDefendantId).isEqualTo(secondDefendantId)

    assertThat(thirdPerson.aliases).isEmpty()
    assertThat(thirdPerson.contacts.size).isEqualTo(0)
    assertThat(thirdPerson.pnc).isEqualTo(PNCIdentifier.from(thirdPnc))
    assertThat(thirdPerson.nationalInsuranceNumber).isEqualTo(thirdDefendantNINumber)
    assertThat(thirdPerson.masterDefendantId).isEqualTo(thirdDefendantId)
  }

  @Test
  fun `should process messages with pnc as empty string and null`() {
    val firstDefendantId = randomUUID().toString()
    val secondDefendantId = randomUUID().toString()

    val messageId = publishCourtMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = firstDefendantId, pnc = ""),
          CommonPlatformHearingSetup(defendantId = secondDefendantId, pnc = null),
        ),
      ),
      COMMON_PLATFORM_HEARING,
    )

    checkTelemetry(
      COURT_MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to HMCTS.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
      times = 2,
    )
    val personWithEmptyPnc = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByDefendantId(firstDefendantId)
    }
    assertThat(personWithEmptyPnc.pnc?.pncId).isEqualTo("")

    val personWithNullPnc = personRepository.findByDefendantId(secondDefendantId)
    assertThat(personWithNullPnc?.pnc?.pncId).isEqualTo("")
  }

  private fun buildPublishRequest(
    defendantId: String,
    pnc: PNCIdentifier,
  ): PublishRequest? = PublishRequest.builder()
    .topicArn(courtEventsTopic?.arn)
    .message(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = defendantId, pnc = pnc.pncId),
          CommonPlatformHearingSetup(defendantId = defendantId, pnc = pnc.pncId),
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
