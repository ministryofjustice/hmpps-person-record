package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC
import java.util.concurrent.TimeUnit.SECONDS

class OffenderDomainEventsListenerIntTest : IntegrationTestBase() {

  @Test
  fun `should receive the message successfully when new offender event published`() {
    // Given
    val crn = "XXX1234"
    val expectedPncNumber = PNCIdentifier.from("2020/0476873U")
    val domainEvent = publishDeliusNewOffenderEvent(NEW_OFFENDER_CREATED, crn)

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(DELIUS_RECORD_CREATION_RECEIVED),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(NEW_DELIUS_RECORD_NEW_PNC),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findPersonEntityByPncNumber(expectedPncNumber) }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(expectedPncNumber)
    assertThat(personEntity.offenders[0].crn).isEqualTo(crn)
  }

  @Test
  fun `should write offender without PNC if PNC is invalid`() {
    val crn = "XXX5678"
    publishDeliusNewOffenderEvent(NEW_OFFENDER_CREATED, crn)

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(DELIUS_RECORD_CREATION_RECEIVED),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    await untilAsserted {
      verify(telemetryService).trackEvent(
        eq(NEW_DELIUS_RECORD_NEW_PNC),
        org.mockito.kotlin.check {
          assertThat(it["CRN"]).isEqualTo(crn)
        },
      )
    }

    val personEntity = await.atMost(10, SECONDS) untilNotNull { personRepository.findByOffendersCrn(crn) }

    assertThat(personEntity.personId).isNotNull()
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].pncNumber?.pncId).isEqualTo("")
    assertThat(personEntity.offenders[0].crn).isEqualTo(crn)
  }
}
