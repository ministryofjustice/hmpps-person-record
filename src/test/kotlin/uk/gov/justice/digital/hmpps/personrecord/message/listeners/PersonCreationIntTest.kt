package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearingWithOneDefendant
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED
import java.util.concurrent.TimeUnit.SECONDS

class PersonCreationIntTest : IntegrationTestBase() {

  @Test
  fun `should allow creation and retrieval of 2 defendants with same PNC and different name`() {
    val pncNumber = "1981/0154257C"
    val crn = "CRN123456"
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber, "Bob", "Marley", "1945-06-02"), COMMON_PLATFORM_HEARING)
    val oneDefendant: List<PersonEntity> = await.atMost(100, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(PNCIdentifier.from(pncNumber)) }

    assertThat(oneDefendant[0].defendants.size).isEqualTo(1)
    // this will create a new defendant
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber), COMMON_PLATFORM_HEARING)
    // send the same message again to make sure it can be handled - this used to fail
    publishHMCTSMessage(commonPlatformHearingWithOneDefendant(pncNumber), COMMON_PLATFORM_HEARING)

    // this should fail if we can get the call to offender search to return the same PNC
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))
    val domainEvent = DomainEvent(eventType = NEW_OFFENDER_CREATED, detailUrl = createDeliusDetailUrl(crn), personReference = personReference, additionalInformation = null)
    publishOffenderDomainEvent(NEW_OFFENDER_CREATED, domainEvent)

    checkTelemetry(NEW_DELIUS_RECORD_PNC_MATCHED, mapOf("PNC" to pncNumber))
  }
}
