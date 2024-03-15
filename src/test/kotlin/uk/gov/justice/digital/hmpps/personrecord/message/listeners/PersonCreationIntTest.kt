package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED
import java.util.concurrent.TimeUnit.SECONDS

class PersonCreationIntTest : IntegrationTestBase() {

  @Test
  fun `should allow creation and retrieval of 2 defendants with same PNC and different name`() {
    val pncNumber = "1981/0154257C"
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber, "Bob", "Marley", "1945-06-02"), COMMON_PLATFORM_HEARING)
    val oneDefendant: PersonEntity = await.atMost(100, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(PNCIdentifier.from(pncNumber)) }

    assertThat(oneDefendant.defendants.size).isEqualTo(1)
    // this will create a new defendant
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber), COMMON_PLATFORM_HEARING)
    // send the same message again to make sure it can be handled - this used to fail
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber), COMMON_PLATFORM_HEARING)

    // this should fail if we can get the call to offender search to return the same PNC
    publishDeliusNewOffenderEvent(NEW_OFFENDER_CREATED, "CRN123456")
    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(NEW_DELIUS_RECORD_PNC_MATCHED),
        org.mockito.kotlin.check {
          assertThat(it["PNC"]).isEqualTo(pncNumber)
        },
      )
    }
    // now fix the code which this assertion calls
//    val twoDefendants: PersonEntity = await.atMost(100, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(PNCIdentifier.from(pncNumber)) }
//
//    assertThat(twoDefendants.defendants.size).isEqualTo(2)
  }
}
