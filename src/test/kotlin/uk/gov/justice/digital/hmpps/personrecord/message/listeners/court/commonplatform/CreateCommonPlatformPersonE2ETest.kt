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
class CreateCommonPlatformPersonE2ETest : MessagingTestBase() {

  @Test
  fun `should create a new person`() {
    val defendantId = randomDefendantId()
    val pnc = randomPnc()
    val cro = randomCro()
    val firstName = randomName()
    val lastName = randomName()

    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = pnc, firstName = firstName, lastName = lastName, cro = cro, defendantId = defendantId))),
    )

    val updatedPersonEntity = awaitNotNullPerson(timeout = 5, function = { personRepository.findByDefendantId(defendantId) })
    assertThat(updatedPersonEntity.getPrimaryName().lastName).isEqualTo(lastName)
    assertThat(updatedPersonEntity.getPnc()).isEqualTo(pnc)
    assertThat(updatedPersonEntity.getCro()).isEqualTo(cro)
    assertThat(updatedPersonEntity.addresses.size).isEqualTo(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "COMMON_PLATFORM", "DEFENDANT_ID" to defendantId),
    )
  }
}
