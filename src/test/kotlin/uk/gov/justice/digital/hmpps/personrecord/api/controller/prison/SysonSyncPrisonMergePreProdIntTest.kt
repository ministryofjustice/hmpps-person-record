package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncAddressesHandler.Companion.toPrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncContactsHandler.Companion.toDto
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupContact

@ActiveProfiles("preprod")
class SysonSyncPrisonMergePreProdIntTest : WebTestBase() {

  @BeforeEach
  fun setup() {
    deleteAllPersonData()
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    stubDeletePersonMatch()
  }

  @Test
  fun `should continue to recreate addresses on the to person equal to what is received in the callback`() {
    val toPrisonNumber = randomPrisonNumber()
    val fromPrisonNumber = randomPrisonNumber()

    createPersonKey()
      .addPerson(
        createRandomPrisonPersonDetails(fromPrisonNumber).copy(
          addresses = listOf(
            Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
            Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
          ),
        ),
      )
      .addPerson(
        createRandomPrisonPersonDetails(toPrisonNumber).copy(
          addresses = listOf(
            Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
            Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
          ),
        ),
      )

    val fromPersonBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
    val toPersonAddressBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.addresses.first()
    val fromAddressBeforeMergeOne = fromPersonBeforeMerge.addresses.first()
    val fromAddressBeforeMergeTwo = fromPersonBeforeMerge.addresses.last()

    val fromPersonAddressReqBodyOne = fromAddressBeforeMergeOne.toPrisonAddress().copy(nomisAddressId = 1L)
    val fromPersonAddressReqBodyTwo = fromAddressBeforeMergeTwo.toPrisonAddress().copy(nomisAddressId = 2L)
    val requestBody = PrisonMerge(
      fromPrisonNumber = fromPrisonNumber,
      toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
        addresses = listOf(fromPersonAddressReqBodyOne, fromPersonAddressReqBodyTwo),
        contacts = emptyList(),
      ),
    )

    val callbackResponse = ApiResponseSetup(prisonNumber = toPrisonNumber, addresses = listOf(ApiResponseSetupAddress(postcode = randomPostcode())))
    prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody, callbackResponse = callbackResponse).returnResult().responseBody!!

    val fromPersonAfterMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
    assertThat(fromPersonAfterMerge.addresses.size).isEqualTo(2)

    val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
    assertThat(toPersonAfterMerge.addresses).hasSize(1)

    val toPersonAddressAfterMerge = toPersonAfterMerge.addresses.first()
    assertThat(toPersonAddressAfterMerge.updateId.toString()).isNotEqualTo(toPersonAddressBeforeMerge.updateId.toString())
  }

  @Test
  fun `should continue to recreate contacts on the to person equal to what is received in the callback`() {
    val toPrisonNumber = randomPrisonNumber()
    val fromPrisonNumber = randomPrisonNumber()

    createPersonKey()
      .addPerson(
        createRandomPrisonPersonDetails(fromPrisonNumber).copy(
          contacts = listOf(
            Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
            Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
          ),
        ),
      )
      .addPerson(
        createRandomPrisonPersonDetails(toPrisonNumber).copy(
          contacts = listOf(
            Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
            Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
          ),
        ),
      )

    val fromPersonBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
    val toPersonContactBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.contacts.first()
    val fromContactBeforeMergeOne = fromPersonBeforeMerge.contacts.first()
    val fromContactBeforeMergeTwo = fromPersonBeforeMerge.contacts.last()

    val fromPersonContactReqBodyOne = fromContactBeforeMergeOne.toDto().copy(nomisContactId = 1L)
    val fromPersonContactReqBodyTwo = fromContactBeforeMergeTwo.toDto().copy(nomisContactId = 2L)
    val requestBody = PrisonMerge(
      fromPrisonNumber = fromPrisonNumber,
      toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
        contacts = listOf(fromPersonContactReqBodyOne, fromPersonContactReqBodyTwo),
        addresses = emptyList(),
      ),
    )

    val callbackResponse = ApiResponseSetup(prisonNumber = toPrisonNumber, contacts = listOf(ApiResponseSetupContact(randomContactType(), randomLowerCaseString())))
    prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody, callbackResponse = callbackResponse).returnResult().responseBody!!

    val fromPersonAfterMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
    assertThat(fromPersonAfterMerge.contacts.size).isEqualTo(2)

    val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
    assertThat(toPersonAfterMerge.contacts).hasSize(1)

    val toPersonContactAfterMerge = toPersonAfterMerge.contacts.first()
    assertThat(toPersonContactAfterMerge.updateId.toString()).isNotEqualTo(toPersonContactBeforeMerge.updateId.toString())
  }

  private fun prisonMergeEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/merge"

  private fun prisonMergeEventAndResponseSetup(
    targetPrisonNumber: String,
    prisonMerge: PrisonMerge,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
    expectedStatus: HttpStatus = HttpStatus.CREATED,
    callbackResponse: ApiResponseSetup = ApiResponseSetup(prisonNumber = targetPrisonNumber),
  ): WebTestClient.BodySpec<SysconPersonMergeResponseBody, *> {
    stubPrisonResponse(
      callbackResponse,
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
}
