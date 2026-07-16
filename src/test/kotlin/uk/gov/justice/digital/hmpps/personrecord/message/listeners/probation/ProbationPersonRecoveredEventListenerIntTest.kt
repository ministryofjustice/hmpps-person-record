package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationPersonRecoveredEventListenerIntTest : ProbationEventListenerTestBase() {

  @Test
  fun `should recreate the person record and all of its addresses`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val crn = randomCrn()
    val probationCase = createRandomProbationCase(crn)
      .copy(addresses = List(3) { randomProbationAddress() })

    stubSingleProbationResponse(ApiResponseSetup.from(probationCase))
    publishProbationPersonRecoveredEvent(crn)

    checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_UPDATED, 3)

    awaitAssert {
      val personEntity = personRepository.findByCrn(crn)
      assertThat(personEntity).isNotNull()
      assertThat(personEntity?.addresses?.size).isEqualTo(3)
    }
  }

  @Test
  fun `should update person and addresses if they already exist in cpr`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()

    val crn = randomCrn()
    val probationCase = createRandomProbationCase(crn)

    val existingProbationAddress = randomProbationAddress()
    val existingPersonEntity = createPersonWithNewKey(
      Person.from(probationCase),
      configure = addAddressToRecord(Address.from(existingProbationAddress)!!),
    )

    assertThat(existingPersonEntity.addresses.size).isEqualTo(1)

    stubSingleProbationResponse(
      ApiResponseSetup.from(
        probationCase.copy(
          selfDescribedGenderIdentity = "RandomSelfDescribedGenderIdentity",
          addresses = listOf(existingProbationAddress.copy(postcode = randomPostcode()), randomProbationAddress()),
        ),
      ),
    )
    publishProbationPersonRecoveredEvent(crn)

    checkEventLogDoesNotExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    checkEventLogExist(crn, CPRLogEvents.CPR_RECORD_UPDATED, 2) // address updates only, no matching fields changed on person

    awaitAssert {
      val personEntity = personRepository.findByCrn(crn)
      assertThat(personEntity).isNotNull()
      assertThat(personEntity?.selfDescribedGenderIdentity).isEqualTo("RandomSelfDescribedGenderIdentity")
      assertThat(personEntity?.addresses?.size).isEqualTo(2)
    }
  }

  @Test
  fun `should not recreate person if person does not exist in delius`() {
    val crn = randomCrn()

    stub404Response(probationUrl(crn))
    publishProbationPersonRecoveredEvent(crn)

    checkEventLogDoesNotExist(crn, CPRLogEvents.CPR_RECORD_CREATED)
    checkEventLogDoesNotExist(crn, CPRLogEvents.CPR_RECORD_UPDATED)

    awaitAssert {
      val personEntity = personRepository.findByCrn(crn)
      assertThat(personEntity).isNull()
    }
    expectNoMessagesOnQueueOrDlq(probationEventsQueue)
  }
}
