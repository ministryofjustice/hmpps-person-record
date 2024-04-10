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
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithAdditionalFields
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithNewDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.helper.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MISSING_PNC
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals

class CourtCaseEventsListenerIntTest : IntegrationTestBase() {

  @Test
  fun `should output correct telemetry for invalid PNC`() {
    val invalidPncNumber = "03/62845X" // X is the incorrect check letter
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(invalidPncNumber), COMMON_PLATFORM_HEARING)

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(INVALID_PNC),
        check {
          assertThat(it["PNC"]).isEqualTo(invalidPncNumber).withFailMessage("PNC incorrect")
        },
      )
    }
  }

  @Test
  fun `should successfully process common platform message with 3 defendants and create correct telemetry events`() {
    publishHMCTSMessage(commonPlatformHearing("19810154257C"), COMMON_PLATFORM_HEARING)

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(HMCTS_MESSAGE_RECEIVED),
        check {
          assertThat(it["PNC"]).isEqualTo("1981/0154257C")
        },
      )
    }
    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(HMCTS_MESSAGE_RECEIVED),
        check {
          assertThat(it["PNC"]).isEqualTo("2008/0056560Z")
        },
      )
    }
    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(HMCTS_MESSAGE_RECEIVED),
        check {
          assertThat(it["PNC"]).isEqualTo("")
        },
      )
    }
  }

  @Test
  fun `should process libra messages with empty pnc identifier`() {
    val emptyPncNumber = ""
    publishHMCTSMessage(libraHearing(pncNumber = emptyPncNumber), LIBRA_COURT_CASE)

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(HMCTS_MESSAGE_RECEIVED),
        check {
          assertThat(it["PNC"]).isEqualTo(emptyPncNumber)
          assertThat(it["CRO"]).isEqualTo("11111/79J")
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(MISSING_PNC),
        check {
          assertThat(it).isEmpty()
        },
      )
    }

    verify(telemetryService, never()).trackEvent(
      eq(INVALID_PNC),
      check {
        assertThat(it["PNC"]).isEqualTo(emptyPncNumber)
      },
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

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
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
      eq(HMCTS_RECORD_CREATED),
      check {
        assertThat(it["PNC"]).isEqualTo(pncNumber.pncId)
      },
    )
  }

  @Test
  fun `should create new defendant and prisoner records with link to a person record from common platform message`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")

    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByPrisonersPncNumber(pncNumber)
    }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.defendants.size).isEqualTo(1)
    assertThat(personEntity.defendants[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.defendants[0].address).isNotNull
    assertThat(personEntity.defendants[0].address?.addressLineOne).isEqualTo("13 broad Street")
    assertThat(personEntity.defendants[0].address?.addressLineTwo).isEqualTo("Cardiff")
    assertThat(personEntity.defendants[0].address?.addressLineThree).isEqualTo("Wales")
    assertThat(personEntity.defendants[0].address?.addressLineFour).isEqualTo("UK")
    assertThat(personEntity.defendants[0].address?.addressLineFive).isEqualTo("Earth")
    assertThat(personEntity.defendants[0].address?.postcode).isEqualTo("CF10 1FU")
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

  @Test
  fun `should create new defendants with additional fields from common platform message`() {
    val pncNumber1 = PNCIdentifier.from("2003/0062845E")
    val pncNumber2 = PNCIdentifier.from("2008/0056560Z")
    val pncNumber3 = PNCIdentifier.from("20230583843L")

    publishHMCTSMessage(commonPlatformHearingWithAdditionalFields(), COMMON_PLATFORM_HEARING)

    val personEntity1 = await.atMost(10, SECONDS) untilNotNull {
      personRepository.findByDefendantsPncNumber(pncNumber1)
    }

    val personEntity2 = await.atMost(10, SECONDS) untilNotNull {
      personRepository.findByDefendantsPncNumber(pncNumber2)
    }

    val personEntity3 = await.atMost(10, SECONDS) untilNotNull {
      personRepository.findByDefendantsPncNumber(pncNumber3)
    }

    assertThat(personEntity1.personId).isNotNull()
    assertThat(personEntity1.defendants.size).isEqualTo(1)
    assertThat(personEntity1.defendants[0].pncNumber).isEqualTo(pncNumber1)
    assertThat(personEntity1.defendants[0].defendantId).isEqualTo("b5cfae34-9256-43ad-87fb-ac3def34e2ac")
    assertThat(personEntity1.defendants[0].masterDefendantId).isEqualTo("eeb71c73-573b-444e-9dc3-4e5998d1be65")
    assertThat(personEntity1.defendants[0].firstName).isEqualTo("Eric")
    assertThat(personEntity1.defendants[0].middleName).isEqualTo("mName1 mName2")
    assertThat(personEntity1.defendants[0].surname).isEqualTo("Lassard")
    assertThat(personEntity1.defendants[0].contact).isNull()
    assertThat(personEntity1.defendants[0].address).isNotNull()
    assertEquals(2, personEntity1.defendants[0].aliases.size)
    assertThat(personEntity1.defendants[0].aliases[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(personEntity1.defendants[0].aliases[0].surname).isEqualTo("alisLastName1")
    assertThat(personEntity1.defendants[0].aliases[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(personEntity1.defendants[0].aliases[1].surname).isEqualTo("alisLastName2")

    assertThat(personEntity2.personId).isNotNull()
    assertThat(personEntity2.defendants.size).isEqualTo(1)
    assertThat(personEntity2.defendants[0].aliases).isEmpty()
    assertThat(personEntity2.defendants[0].address).isNotNull()
    assertThat(personEntity2.defendants[0].pncNumber).isEqualTo(pncNumber2)
    assertThat(personEntity2.defendants[0].pncNumber).isEqualTo(pncNumber2)
    assertThat(personEntity2.defendants[0].contact?.homePhone).isEqualTo("0207345678")
    assertThat(personEntity2.defendants[0].contact?.workPhone).isEqualTo("0203788776")
    assertThat(personEntity2.defendants[0].contact?.mobile).isEqualTo("078590345677")
    assertThat(personEntity2.defendants[0].contact?.primaryEmail).isEqualTo("email@email.com")
    assertThat(personEntity2.defendants[0].defendantId).isEqualTo("cc36c035-6e82-4d04-94c2-2a5728f11481")
    assertThat(personEntity2.defendants[0].masterDefendantId).isEqualTo("1f6847a2-6663-44dd-b945-fe2c20961d0a")

    assertThat(personEntity3.personId).isNotNull()
    assertThat(personEntity3.defendants.size).isEqualTo(1)
    assertThat(personEntity3.defendants[0].aliases).isEmpty()
    assertThat(personEntity3.defendants[0].contact).isNull()
    assertThat(personEntity3.defendants[0].pncNumber).isEqualTo(pncNumber3)
    assertThat(personEntity3.defendants[0].nationalityCode).isNull()
    assertThat(personEntity3.defendants[0].sex).isNull()
    assertThat(personEntity3.defendants[0].nationalityOne).isNull()
    assertThat(personEntity3.defendants[0].nationalityTwo).isNull()
    assertThat(personEntity3.defendants[0].observedEthnicityDescription).isNull()
    assertThat(personEntity3.defendants[0].selfDefinedEthnicityDescription).isNull()
    assertThat(personEntity3.defendants[0].nationalInsuranceNumber).isEqualTo("PC456743D")
    assertThat(personEntity3.defendants[0].defendantId).isEqualTo("b56f8612-0f4c-43e5-840a-8bedb17722ec")
    assertThat(personEntity3.defendants[0].masterDefendantId).isEqualTo("290e0457-1480-4e62-b3c8-7f29ef791c58")
  }

  @Test
  fun `should create offender with additional fields`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")

    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByPrisonersPncNumber(pncNumber)
    }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].crn).isEqualTo("X026350")
    assertThat(personEntity.offenders[0].offenderId).isEqualTo(2500034487)
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.offenders[0].firstName).isEqualTo("Eric")
    assertThat(personEntity.offenders[0].lastName).isEqualTo("Lassard")
    assertThat(personEntity.offenders[0].gender).isNull()
    assertThat(personEntity.offenders[0].ethnicity).isNull()
    assertThat(personEntity.offenders[0].nationality).isNull()
    assertThat(personEntity.offenders[0].dateOfBirth).isEqualTo(LocalDate.of(1960, 1, 1))
    assertThat(personEntity.offenders[0].prisonNumber).isEqualTo("A1671AJ")
    assertThat(personEntity.offenders[0].nationalInsuranceNumber).isEqualTo("Ab123456G")
    assertThat(personEntity.offenders[0].mostRecentPrisonNumber).isEqualTo("2345")
    assertThat(personEntity.offenders[0].contact?.homePhone).isEqualTo("02920345665")
    assertThat(personEntity.offenders[0].contact?.mobile).isEqualTo("07123456789")
    assertThat(personEntity.offenders[0].address).isNotNull()
    assertThat(personEntity.offenders[0].address?.postcode).isEqualTo("NF1 1NF")
    assertEquals(1, personEntity.offenders[0].aliases.size)
    assertThat(personEntity.offenders[0].aliases[0].firstName).isEqualTo("aliasFirstName")
    assertThat(personEntity.offenders[0].aliases[0].middleName).isEqualTo("mName1 mName2")
    assertThat(personEntity.offenders[0].aliases[0].dateOfBirth).isEqualTo(LocalDate.of(1968, 2, 22))
    assertThat(personEntity.offenders[0].aliases[0].surname).isEqualTo("alisSurName")
    assertThat(personEntity.offenders[0].aliases[0].aliasOffenderId).isEqualTo("12345")
  }

  @Test
  fun `should create prisoner with additional fields`() {
    val pncNumber = PNCIdentifier.from("2003/0062845E")

    publishHMCTSMessage(commonPlatformHearingWithNewDefendant(), COMMON_PLATFORM_HEARING)

    val personEntity = await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByPrisonersPncNumber(pncNumber)
    }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.prisoners).hasSize(1)
    assertThat(personEntity.prisoners[0].firstName).isEqualTo("ERIC")
    assertThat(personEntity.prisoners[0].lastName).isEqualTo("Lassard")
    assertThat(personEntity.prisoners[0].prisonNumber).isEqualTo("A1234AA")
    assertThat(personEntity.prisoners[0].pncNumber).isEqualTo(pncNumber)
    assertThat(personEntity.prisoners[0].offenderId).isEqualTo(356)
    assertThat(personEntity.prisoners[0].rootOffenderId).isEqualTo(300)
    assertThat(personEntity.prisoners[0].dateOfBirth).isEqualTo(LocalDate.of(1970, 3, 15))
    assertThat(personEntity.prisoners[0].cro).isEqualTo("CR1234")
    assertThat(personEntity.prisoners[0].drivingLicenseNumber).isEqualTo("ERIC1234567K")
    assertThat(personEntity.prisoners[0].nationalInsuranceNumber).isEqualTo("PD123456D")
    assertThat(personEntity.prisoners[0].address?.postcode).isEqualTo("LI1 5TH")
    assertThat(personEntity.prisoners[0].sexCode).isNull()
    assertThat(personEntity.prisoners[0].raceCode).isNull()
    assertThat(personEntity.prisoners[0].birthPlace).isNull()
    assertThat(personEntity.prisoners[0].birthCountryCode).isNull()
  }
}
