package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant

class PersonCreationIntTest : IntegrationTestBase() {

  @Test
  fun `should allow creation and retrieval of 2 defendants with same PNC and different name`() {
    val pncNumber = "1981/0154257C"
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber, "Bob"), COMMON_PLATFORM_HEARING)

    publishDeliusNewOffenderEvent(createDomainEvent(NEW_OFFENDER_CREATED, "C1234"))
  }
}
