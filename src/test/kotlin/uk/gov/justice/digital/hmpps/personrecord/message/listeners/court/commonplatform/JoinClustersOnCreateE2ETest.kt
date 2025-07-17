package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc

@ActiveProfiles("e2e")
class JoinClustersOnCreateE2ETest : MessagingTestBase() {

  @Test
  fun `should create a new person which matches two existing people in different clusters and join all three on a single cluster`() {
    val firstDefendantId = randomDefendantId()
    val secondDefendantId = randomDefendantId()
    val thirdDefendantId = randomDefendantId()
    val pnc = randomPnc()
    val cro = randomCro()
    val firstName = randomName()
    val lastName = randomName()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = pnc, firstName = firstName, lastName = lastName, defendantId = firstDefendantId))),
    )

    val firstDefendant = awaitNotNullPerson(timeout = 7, function = { personRepository.findByDefendantId(firstDefendantId) })
    assertThat(firstDefendant.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(firstDefendant.getPnc()).isEqualTo(pnc)
    assertThat(firstDefendant.addresses.size).isEqualTo(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to firstDefendantId),
    )

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(cro = cro, firstName = firstName, lastName = lastName, defendantId = secondDefendantId))),
    )

    val secondDefendant = awaitNotNullPerson(timeout = 7, function = { personRepository.findByDefendantId(secondDefendantId) })
    assertThat(secondDefendant.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(secondDefendant.getCro()).isEqualTo(cro)
    assertThat(secondDefendant.personKey!!.personUUID).isNotEqualTo(firstDefendant.personKey!!.personUUID)

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(cro = cro, pnc = pnc, firstName = firstName, lastName = lastName, defendantId = thirdDefendantId))),
    )

    val thirdDefendant = awaitNotNullPerson(timeout = 7, function = { personRepository.findByDefendantId(thirdDefendantId) })
    assertThat(thirdDefendant.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(thirdDefendant.getCro()).isEqualTo(cro)
    assertThat(thirdDefendant.getPnc()).isEqualTo(pnc)
    assertThat(thirdDefendant.personKey!!.personEntities.size).isEqualTo(3)
    assertThat(secondDefendant.personKey!!.personUUID).isEqualTo(firstDefendant.personKey!!.personUUID)
    assertThat(firstDefendant.personKey!!.personUUID).isEqualTo(thirdDefendant.personKey!!.personUUID)
  }
}
