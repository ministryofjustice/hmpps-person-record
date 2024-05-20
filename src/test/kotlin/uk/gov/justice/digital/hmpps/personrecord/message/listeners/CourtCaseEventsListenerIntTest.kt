package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.untilNotNull
import org.jmock.lib.concurrent.Blitzer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithAdditionalFields
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendantAndNoPnc
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithSameDefendantIdTwice
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MULTIPLE_RECORDS_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

class CourtCaseEventsListenerIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `should successfully process common platform message with 3 defendants and create correct telemetry events`() {
    val id1 = UUID.randomUUID().toString()
    val id2 = UUID.randomUUID().toString()
    val id3 = UUID.randomUUID().toString()
    val messageId = publishHMCTSMessage(commonPlatformHearing(defendantIds = listOf(id1, id2, id3), pncNumber = "19810154257C"), COMMON_PLATFORM_HEARING)

    await.atMost(10, SECONDS) untilNotNull {
      assertThat(personRepository.findByDefendantId(id1))
    }

    await.atMost(10, SECONDS) untilNotNull {
      assertThat(personRepository.findByDefendantId(id2))
    }

    await.atMost(10, SECONDS) untilNotNull {
      assertThat(personRepository.findByDefendantId(id3))
    }

    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to "1981/0154257C", "MESSAGE_ID" to messageId),
    )
    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to "2008/0056560Z", "MESSAGE_ID" to messageId),
    )
    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("PNC" to "", "MESSAGE_ID" to messageId),
    )
  }

  @Test
  fun `should not push messages from Common Platform onto dead letter queue when processing fails because of could not serialize access due to read write dependencies among transactions`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")
    val defendantId = UUID.randomUUID().toString()
    val publishRequest = PublishRequest.builder()
      .topicArn(courtCaseEventsTopic?.arn)
      .message(commonPlatformHearingWithSameDefendantIdTwice(defendantId = defendantId, pncNumber = pncNumber.pncId))
      .messageAttributes(
        mapOf(
          "messageType" to MessageAttributeValue.builder().dataType("String")
            .stringValue(COMMON_PLATFORM_HEARING.name).build(),
        ),
      )
      .build()
    val blitzer = Blitzer(100, 10)
    try {
      blitzer.blitz {
        courtCaseEventsTopic?.snsClient?.publish(publishRequest)?.get()
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
      mapOf("SourceSystem" to "HMCTS", "DefendantId" to defendantId),
    )
//    checkTelemetry(
//      CPR_RECORD_UPDATED,
//      mapOf("SourceSystem" to "HMCTS"),
//      times(199),
//    )
  }

  @Test
  fun `should choose one and update when there are duplicate defendants - bug fix for CPR-331`() {
    val id1 = UUID.randomUUID().toString()
    val id2 = UUID.randomUUID().toString()
    val id3 = UUID.randomUUID().toString()
    personRepository.saveAndFlush(PersonEntity.from(Person.from(Defendant(id = id1))))
    personRepository.saveAndFlush(PersonEntity.from(Person.from(Defendant(id = id1))))

    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(defendantIds = listOf(id1, id2, id3)), COMMON_PLATFORM_HEARING)

    checkTelemetry(
      HMCTS_MESSAGE_RECEIVED,
      mapOf("CRO" to "051072/62R", "PNC" to "2003/0062845E"),
    )

    checkTelemetry(
      CPR_MULTIPLE_RECORDS_FOUND,
      mapOf("SourceSystem" to "HMCTS", "DefendantId" to id1),
    )

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SourceSystem" to "HMCTS", "DefendantId" to id1),
    )

    val personEntities = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findAllByDefendantId(id1)
    }
    assertThat(personEntities.size).isEqualTo(2)
  }

  @Test
  fun `should create new person record from common platform message`() {
    val id1 = UUID.randomUUID().toString()
    val id2 = UUID.randomUUID().toString()
    val id3 = UUID.randomUUID().toString()
    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(defendantIds = listOf(id1, id2, id3)), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id1)
    }

    assertThat(personEntity.pnc).isEqualTo(PNCIdentifier.from("2003/0062845E"))
    assertThat(personEntity.cro).isEqualTo(CROIdentifier.from("51072/62R"))
    assertThat(personEntity.fingerprint).isEqualTo(true)
    assertThat(personEntity.addresses.size).isEqualTo(1)
    assertThat(personEntity.addresses[0].postcode).isEqualTo("CF10 1FU")
  }

  @Test
  fun `should update an existing person record from common platform message`() {
    val defendantId = UUID.randomUUID().toString()
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(defendantId = defendantId), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    assertThat(personEntity.lastName).isEqualTo("Andy")
    assertThat(personEntity.addresses.size).isEqualTo(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SourceSystem" to "HMCTS", "DefendantId" to defendantId),
    )

    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(defendantId = defendantId, lastName = "Smith"), COMMON_PLATFORM_HEARING)

    val updatedPersonEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByDefendantId(defendantId)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SourceSystem" to "HMCTS", "DefendantId" to defendantId),
    )

    assertThat(updatedPersonEntity.lastName).isEqualTo("Smith")
    assertThat(updatedPersonEntity.pnc).isEqualTo(PNCIdentifier.from("1981/0154257C"))
    assertThat(updatedPersonEntity.cro).isEqualTo(CROIdentifier.from("86621/65B"))
    assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
  }

  @Test
  fun `should create new people with additional fields from common platform message`() {
    val pncNumber1 = PNCIdentifier.from("2003/0062845E")
    val pncNumber2 = PNCIdentifier.from("2008/0056560Z")
    val pncNumber3 = PNCIdentifier.from("20230583843L")

    val id1 = UUID.randomUUID().toString()
    val id2 = UUID.randomUUID().toString()
    val id3 = UUID.randomUUID().toString()

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
    val id1 = UUID.randomUUID().toString()
    val id2 = UUID.randomUUID().toString()
    publishHMCTSMessage(commonPlatformHearingWithNewDefendantAndNoPnc(defendantIds = listOf(id1, id2)), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(15, SECONDS) untilNotNull {
      personRepository.findByDefendantId(id1)
    }
    assertThat(personEntity.pnc?.pncId).isEqualTo("")

    val secondPersonEntity = personRepository.findByDefendantId(id2)
    assertThat(secondPersonEntity?.pnc?.pncId).isEqualTo("")
    assertThat(secondPersonEntity?.cro?.croId).isEqualTo("075715/64Q")
  }
}
