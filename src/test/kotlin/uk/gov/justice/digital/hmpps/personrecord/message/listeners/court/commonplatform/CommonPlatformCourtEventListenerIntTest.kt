package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupContact
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.largeCommonPlatformMessage
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc

class CommonPlatformCourtEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `FIFO queue and topic remove duplicate messages`() {
    val pnc = randomPnc()
    val defendantId = randomDefendantId()
    val firstName = randomName()
    val lastName = randomName()

    blitz(30, 6) {
      publishCommonPlatformMessage(
        commonPlatformHearing(
          listOf(
            CommonPlatformHearingSetup(
              pnc = pnc,
              defendantId = defendantId,
              firstName = firstName,
              lastName = lastName,
              cro = "",
              nationalInsuranceNumber = "NINO",
              hearingId = "HEARING1234",
            ),
            CommonPlatformHearingSetup(
              pnc = pnc,
              defendantId = defendantId,
              firstName = firstName,
              lastName = lastName,
              cro = "",
              nationalInsuranceNumber = "NINO",
              hearingId = "HEARING1234",
            ),
          ),
        ),

      )
    }

    expectNoMessagesOn(courtEventsQueue)
    expectNoMessagesOnDlq(courtEventsQueue)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf(
        "SOURCE_SYSTEM" to "COMMON_PLATFORM",
        "DEFENDANT_ID" to defendantId,
      ),
      0,
    )
  }

  @Test
  fun `should update an existing person record from common platform message`() {
    val defendantId = randomDefendantId()
    val pnc = randomPnc()
    val cro = randomCro()
    val firstName = randomName()
    val lastName = randomName()

    val personKey = createPersonKey()
    createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(Reference(PNC, pnc), Reference(CRO, cro)),
        firstName = firstName,
        lastName = lastName,
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKey,
    )

    val changedLastName = randomName()
    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = pnc, lastName = changedLastName, cro = cro, defendantId = defendantId))),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "DEFENDANT_ID" to defendantId),
    )

    awaitAssert {
      val updatedPersonEntity = personRepository.findByDefendantId(defendantId)!!
      assertThat(updatedPersonEntity.lastName).isEqualTo(changedLastName)
      assertThat(updatedPersonEntity.references.getType(PNC).first().identifierValue).isEqualTo(pnc)
      assertThat(updatedPersonEntity.references.getType(CRO).first().identifierValue).isEqualTo(cro)
      assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)
    }

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personKey.personId.toString()),
    )
  }

  @Test
  fun `should create new people with additional fields from common platform message`() {
    val firstPnc = randomPnc()
    val firstName = randomName()
    val lastName = randomName()
    val secondPnc = randomPnc()
    val thirdPnc = randomPnc()

    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()
    val thirdDefendantId = randomDefendantId()

    val thirdDefendantNINumber = randomNationalInsuranceNumber()

    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            pnc = firstPnc,
            firstName = firstName,
            middleName = "mName1 mName2",
            lastName = lastName,
            defendantId = firstDefendantId,
            aliases = listOf(
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName1", lastName = "alisLastName1"),
              CommonPlatformHearingSetupAlias(firstName = "aliasFirstName2", lastName = "alisLastName2"),
            ),
          ),
          CommonPlatformHearingSetup(pnc = secondPnc, defendantId = secondDefendantId, contact = CommonPlatformHearingSetupContact()),
          CommonPlatformHearingSetup(pnc = thirdPnc, defendantId = thirdDefendantId, nationalInsuranceNumber = thirdDefendantNINumber),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to firstDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
      ),
    )
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to secondDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
      ),
    )
    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf(
        "DEFENDANT_ID" to thirdDefendantId,
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
      ),
    )
    val firstPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(firstDefendantId)
    }

    val secondPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(secondDefendantId)
    }

    val thirdPerson = awaitNotNullPerson {
      personRepository.findByDefendantId(thirdDefendantId)
    }

    assertThat(firstPerson.references.getType(PNC).first().identifierValue).isEqualTo(firstPnc)
    assertThat(firstPerson.personKey).isNotNull()
    assertThat(firstPerson.masterDefendantId).isEqualTo(firstDefendantId)
    assertThat(firstPerson.firstName).isEqualTo(firstName)
    assertThat(firstPerson.middleNames).isEqualTo("mName1 mName2")
    assertThat(firstPerson.lastName).isEqualTo(lastName)
    assertThat(firstPerson.contacts).isEmpty()
    assertThat(firstPerson.addresses).isNotEmpty()
    assertThat(firstPerson.pseudonyms.size).isEqualTo(2)
    assertThat(firstPerson.pseudonyms[0].firstName).isEqualTo("aliasFirstName1")
    assertThat(firstPerson.pseudonyms[0].lastName).isEqualTo("alisLastName1")
    assertThat(firstPerson.pseudonyms[1].firstName).isEqualTo("aliasFirstName2")
    assertThat(firstPerson.pseudonyms[1].lastName).isEqualTo("alisLastName2")

    assertThat(secondPerson.pseudonyms).isEmpty()
    assertThat(secondPerson.addresses).isNotEmpty()
    assertThat(secondPerson.addresses[0].postcode).isEqualTo("CF10 1FU")
    assertThat(secondPerson.references.getType(PNC).first().identifierValue).isEqualTo(secondPnc)
    assertThat(secondPerson.contacts.size).isEqualTo(3)
    assertThat(secondPerson.contacts[0].contactType).isEqualTo(HOME)
    assertThat(secondPerson.contacts[0].contactValue).isEqualTo("0207345678")
    assertThat(secondPerson.contacts[1].contactType).isEqualTo(MOBILE)
    assertThat(secondPerson.contacts[1].contactValue).isEqualTo("078590345677")
    assertThat(secondPerson.masterDefendantId).isEqualTo(secondDefendantId)

    assertThat(thirdPerson.pseudonyms).isEmpty()
    assertThat(thirdPerson.contacts.size).isEqualTo(0)
    assertThat(thirdPerson.references.getType(PNC).first().identifierValue).isEqualTo(thirdPnc)
    assertThat(thirdPerson.references.getType(NATIONAL_INSURANCE_NUMBER).first().identifierValue).isEqualTo(thirdDefendantNINumber)
    assertThat(thirdPerson.masterDefendantId).isEqualTo(thirdDefendantId)
  }

  @Test
  fun `should log Message Processing Failed telemetry event when an exception is thrown`() {
    val messageId = publishCommonPlatformMessage(
      "notAValidMessage",

    )

    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name),
    )
  }

  @Test
  fun `should process large messages`() {
    val firstDefendantId = randomDefendantId()
    val messageId = publishCommonPlatformMessage(
      largeCommonPlatformMessage(),
    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
    )
    awaitNotNullPerson { personRepository.findByDefendantId(firstDefendantId) }
  }

  @Test
  fun `should process messages with pnc as empty string and null`() {
    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()

    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(pnc = "", defendantId = firstDefendantId),
          CommonPlatformHearingSetup(pnc = null, defendantId = secondDefendantId),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
      times = 2,
    )
    val personWithEmptyPnc = awaitNotNullPerson {
      personRepository.findByDefendantId(firstDefendantId)
    }
    assertThat(personWithEmptyPnc.references.getType(PNC)).isEqualTo(emptyList<ReferenceEntity>())

    val personWithNullPnc = personRepository.findByDefendantId(secondDefendantId)
    assertThat(personWithNullPnc?.references?.getType(PNC)).isEqualTo(emptyList<ReferenceEntity>())
  }

  @Test
  fun `should not process youth cases`() {
    val youthDefendantId = randomDefendantId()
    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = youthDefendantId, isYouth = true),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
      times = 0,
    )
  }

  @Test
  fun `should process when is youth is null`() {
    val youthDefendantId = randomDefendantId()
    val messageId = publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(defendantId = youthDefendantId, isYouth = null),
        ),
      ),

    )

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("MESSAGE_ID" to messageId, "SOURCE_SYSTEM" to COMMON_PLATFORM.name, "EVENT_TYPE" to COMMON_PLATFORM_HEARING.name),
    )
  }
}
