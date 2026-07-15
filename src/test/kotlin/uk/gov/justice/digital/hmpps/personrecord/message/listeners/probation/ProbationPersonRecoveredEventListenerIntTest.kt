package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationPersonRecoveredEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `should `() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val crn = randomCrn()
    val probationCase = createRandomProbationCase(crn)
      .copy(addresses = List(3) { ProbationAddress(postcode = randomPostcode(), deliusAddressId = randomDeliusAddressId()) })

    stubSingleProbationResponse(ApiResponseSetup.from(probationCase))
    publishProbationPersonRecoveredEvent(crn)

    checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)

    awaitAssert {
      val personEntity = personRepository.findByCrn(crn)
      assertThat(personEntity).isNotNull()
      assertThat(personEntity?.addresses?.size).isEqualTo(3)
    }
  }
}
