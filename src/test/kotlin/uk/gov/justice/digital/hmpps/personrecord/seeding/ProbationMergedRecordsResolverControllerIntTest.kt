package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationMergedRecordsResolverControllerIntTest : WebTestBase() {

  @Test
  fun `should delete records and recreate the existing Delius record`() {
    val personToDelete = createPerson(createRandomProbationPersonDetails())
    val existingPersonKey = createPersonKey()
      .addPerson(personToDelete)
      .addPerson(createRandomProbationPersonDetails())

    val personDetails = createRandomProbationCase()
    val personToRecreate = createPerson(Person.from(personDetails)) { mergedTo = personToDelete.id }
    personToRecreate.assertMergedTo(personToDelete)
    existingPersonKey.assertClusterIsOfSize(2)

    stubDeletePersonMatch()
    stubPersonMatchScores()
    stubPersonMatchUpsert()
    val apiResponse = ApiResponseSetup.from(
      personDetails.copy(
        selfDescribedGenderIdentity = "RandomSelfDescribedGenderIdentity",
        addresses = List(3) { ProbationAddress(postcode = randomPostcode(), deliusAddressId = randomDeliusAddressId()) },
      ),
    )
    stubSingleProbationResponse(apiResponse)

    webTestClient.post()
      .uri(ADMIN_RESOLVE_MERGED_RECORDS_URL)
      .contentType(APPLICATION_JSON)
      .bodyValue(ResolveMergedRecordsConfig(personToDelete.crn!!, personToRecreate.crn!!))
      .exchange()
      .expectStatus()
      .isOk

    checkEventLogExist(personToDelete.crn!!, CPRLogEvents.CPR_RECORD_DELETED)
    checkEventLogExist(personToRecreate.crn!!, CPRLogEvents.CPR_RECORD_DELETED)
    checkEventLogExist(personToRecreate.crn!!, CPRLogEvents.CPR_RECORD_CREATED)
    existingPersonKey.assertClusterIsOfSize(1)

    awaitAssert {
      val recreatedPerson = personRepository.findByCrn(personToRecreate.crn!!)!!
      assertThat(recreatedPerson).isNotNull()
      recreatedPerson.assertNotMerged()
      assertThat(recreatedPerson.selfDescribedGenderIdentity).isEqualTo("RandomSelfDescribedGenderIdentity")
      assertThat(recreatedPerson.addresses.size).isEqualTo(3)
    }
    assertThat(personRepository.findByCrn(personToDelete.crn!!)).isNull()
  }

  companion object {
    private const val ADMIN_RESOLVE_MERGED_RECORDS_URL = "/admin/resolve-merged-records"
  }
}
