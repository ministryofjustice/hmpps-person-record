package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.Companion.validPrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

@ActiveProfiles("prod")
class SysonSyncPrisonMergeProdIntTest : WebTestBase() {

  @Test
  fun `should ignore addresses and contacts upon a merge`() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    stubDeletePersonMatch()

    val toPrisonNumber = randomPrisonNumber()
    val fromPrisonNumber = randomPrisonNumber()

    createPersonKey()
      .addPerson(
        createRandomPrisonPersonDetails(fromPrisonNumber).copy(
          addresses = listOf(
            Address(postcode = randomPostcode()),
            Address(postcode = randomPostcode()),
          ),
          contacts = listOf(
            Contact(contactType = randomContactType(), contactValue = randomLowerCaseString()),
            Contact(contactType = randomContactType(), contactValue = randomLowerCaseString()),
          ),
        ),
      )
      .addPerson(
        createRandomPrisonPersonDetails(toPrisonNumber).copy(
          addresses = listOf(Address(postcode = randomPostcode())),
          contacts = listOf(Contact(contactType = randomContactType(), contactValue = randomLowerCaseString())),
        ),
      )

    val fromPersonBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
    val toPersonBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
    assertThat(fromPersonBeforeMerge.addresses.size).isEqualTo(2)
    assertThat(fromPersonBeforeMerge.contacts.size).isEqualTo(2)
    assertThat(toPersonBeforeMerge.addresses.size).isEqualTo(1)
    assertThat(toPersonBeforeMerge.contacts.size).isEqualTo(1)

    val requestBody = validPrisonMerge(fromPrisonNumber)
    prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

    val fromPersonAfterMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
    assertThat(fromPersonAfterMerge.addresses.size).isEqualTo(2)
    assertThat(fromPersonAfterMerge.contacts.size).isEqualTo(2)

    val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
    assertThat(toPersonAfterMerge.addresses).hasSize(1)
    assertThat(toPersonAfterMerge.contacts).hasSize(1)
  }

  private fun prisonMergeEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/merge"

  private fun prisonMergeEventAndResponseSetup(
    targetPrisonNumber: String,
    prisonMerge: PrisonMerge,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
    expectedStatus: HttpStatus = HttpStatus.CREATED,
  ): WebTestClient.BodySpec<SysconPersonMergeResponseBody, *> {
    stubPrisonResponse(
      ApiResponseSetup(prisonNumber = targetPrisonNumber),
      scenario,
      currentScenarioState,
      nextScenarioState,
    )
    return publishPrisonPersonMergedEvent<SysconPersonMergeResponseBody>(targetPrisonNumber, prisonMerge, expectedStatus)
  }

  private inline fun <reified T : Any> publishPrisonPersonMergedEvent(
    targetPrisonNumber: String,
    prisonMerge: PrisonMerge,
    expectedStatus: HttpStatus = HttpStatus.CREATED,
  ): WebTestClient.BodySpec<T, *> = sendPostRequestAsserted<T>(
    url = prisonMergeEndpoint(targetPrisonNumber),
    body = prisonMerge,
    roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
    expectedStatus = expectedStatus,
  )

  private fun validPrisonMerge(fromPrisonNumber: String) = PrisonMerge(
    fromPrisonNumber = fromPrisonNumber,
    toPrisonAddressesAndContactsRequest = validPrisonAddressesAndContactsRequest,
  )
}
