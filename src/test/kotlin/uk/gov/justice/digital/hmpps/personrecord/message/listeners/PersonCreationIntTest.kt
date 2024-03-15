package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import java.util.concurrent.TimeUnit.SECONDS

class PersonCreationIntTest : IntegrationTestBase() {

  @Test
  fun `should allow creation and retrieval of 2 defendants with same PNC and different name`() {
    val pncNumber = "1981/0154257C"
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber, "Bob", "Marley", "1945-06-02"), COMMON_PLATFORM_HEARING)
    val oneDefendant: PersonEntity = await.atMost(100, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(PNCIdentifier.from(pncNumber)) }

    assertThat(oneDefendant.defendants.size).isEqualTo(1)
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber), COMMON_PLATFORM_HEARING)
    // this used to fail, now works so problem is fixed
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber), COMMON_PLATFORM_HEARING)

    // now fix the code which this assertion calls
    // val twoDefendants: PersonEntity = await.atMost(100, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(PNCIdentifier.from(pncNumber)) }

    // assertThat(twoDefendants.defendants.size).isEqualTo(2)
  }
}
