package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.Companion.validPrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncAddressesHandler.Companion.toPrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import kotlin.jvm.optionals.getOrNull

class SysconSyncPrisonMergeAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class Merging {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event returns mappings`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      val response = prisonMergeEventAndResponseSetup(targetPrisonNumber = targetPrisonNumber, prisonMerge = validPrisonMerge(sourcePrisonNumber)).returnResult().responseBody!!
      assertThat(response.toPrisonNumber).isEqualTo(targetPrisonNumber)
    }

    @Test
    fun `should overwrite all addresses for to person after a merge and return correct id mappings`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(
          createRandomPrisonPersonDetails(fromPrisonNumber).copy(
            addresses = listOf(
              Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
              Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
            ),
          ),
        )
        .addPerson(
          createRandomPrisonPersonDetails(toPrisonNumber).copy(
            addresses = listOf(
              Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
            ),
          ),
        )

      val fromPersonBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
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

      val actualResponseBody = prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

      val fromPersonAfterMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
      assertThat(fromPersonAfterMerge.addresses.size).isEqualTo(2)

      val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      assertThat(toPersonAfterMerge.addresses).hasSize(2)

      val toPersonAddressAfterMergeOne = toPersonAfterMerge.addresses.first { it.fullAddress == fromAddressBeforeMergeOne.fullAddress }
      assertThat(toPersonAddressAfterMergeOne.updateId).isEqualTo(fromAddressBeforeMergeOne.updateId)
      assertAddressMatches(fromPersonAddressReqBodyOne, toPersonAddressAfterMergeOne)

      val toPersonAddressAfterMergeTwo = toPersonAfterMerge.addresses.first { it.fullAddress == fromAddressBeforeMergeTwo.fullAddress }
      assertThat(toPersonAddressAfterMergeTwo.updateId).isEqualTo(fromAddressBeforeMergeTwo.updateId)
      assertAddressMatches(fromPersonAddressReqBodyOne, toPersonAddressAfterMergeTwo)

      val expectedBody = SysconPersonMergeResponseBody(
        toPrisonNumber = toPrisonNumber,
        sysconAddressesAndContactsResponseBody = SysconAddressesAndContactsResponseBody(
          prisonNumber = toPrisonNumber,
          addressesMappings = listOf(
            SysconAddressMapping(
              nomisAddressId = fromPersonAddressReqBodyOne.nomisAddressId!!,
              cprAddressId = toPersonAddressAfterMergeOne.updateId.toString(),
              addressUsageMappings = emptyList(),
              contactMappings = emptyList(),
            ),
          ),
          contactMappings = emptyList(),
        ),
      )
      assertThat(actualResponseBody).isEqualTo(expectedBody)
    }

    @Test
    fun `should overwrite all contacts for to person after a merge and return correct id mappings`() {
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event with records with same UUID is published`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(targetPrisonNumber = targetPrisonNumber, prisonMerge = validPrisonMerge(sourcePrisonNumber))

      awaitAssert {
        val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)!!
        val targetPerson = personRepository.findByPrisonNumber(targetPrisonNumber)!!
        sourcePerson.assertNotLinkedToCluster()
        sourcePerson.assertMergedTo(targetPerson)
        targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
        targetPerson.personKey?.assertClusterIsOfSize(1)
      }

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has multiple records`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourceCluster = createPersonKey()
        .addPerson(Person(prisonNumber = randomPrisonNumber(), sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))

      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber))
      awaitAssert {
        val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)!!
        sourcePerson.assertNotLinkedToCluster()
        sourcePerson.assertMergedTo(targetPerson)

        targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
        targetPerson.personKey?.assertClusterIsOfSize(1)

        sourceCluster.assertClusterIsOfSize(1)
        sourceCluster.assertClusterStatus(UUIDStatusType.ACTIVE)

        targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
        targetPerson.personKey?.assertClusterIsOfSize(1)
      }

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has a single record`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      val sourceClusterId = sourcePerson.personKey?.id

      prisonMergeEventAndResponseSetup(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber))

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      val sourceClusterPostMerge = personKeyRepository.findById(sourceClusterId!!).getOrNull()
      assertThat(sourceClusterPostMerge).isNull()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLog(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
        assertThat(eventLogs.first().personUUID).isEqualTo(sourcePerson.personKey!!.personUUID)
      }
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `should fail with a 500 error when message processing fails`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure")

      publishPrisonPersonMergedEvent<Unit>(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber), HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  @Nested
  inner class PassiveStateRecords {

    @BeforeEach
    fun beforeEach() {
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event and target record passive state is maintained`() {
      stubPersonMatchScores()
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }

      prisonMergeEventAndResponseSetup(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber))

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      assertThat(sourcePerson.isPassive()).isFalse()
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
      assertThat(targetPerson.isPassive()).isTrue()
      wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `processes prisoner merge event and source record passive state is not maintained on target record`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(
        targetPrisonNumber,
        validPrisonMerge(sourcePrisonNumber),
      )

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      assertThat(sourcePerson.isPassive()).isTrue()
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
      assertThat(targetPerson.isPassive()).isFalse()
      wiremock.verify(1, postRequestedFor(urlEqualTo("/person")))
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class Authorisation {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = validPrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = validPrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonMergeEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/merge"
  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

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

  companion object {
    fun assertAddressMatches(request: PrisonAddress, entity: AddressEntity): Unit = with(entity) {
      assertThat(fullAddress).isEqualTo(request.fullAddress)
      assertThat(noFixedAbode).isEqualTo(request.noFixedAbode)
      assertThat(startDate?.toUkLocalDate()).isEqualTo(request.startDate)
      assertThat(endDate?.toUkLocalDate()).isEqualTo(request.endDate)
      assertThat(postcode).isEqualTo(request.postcode)
      assertThat(subBuildingName).isEqualTo(request.subBuildingName)
      assertThat(buildingName).isEqualTo(request.buildingName)
      assertThat(buildingNumber).isEqualTo(request.buildingNumber)
      assertThat(thoroughfareName).isEqualTo(request.thoroughfareName)
      assertThat(dependentLocality).isEqualTo(request.dependentLocality)
      assertThat(postTown).isEqualTo(request.postTown)
      assertThat(county).isEqualTo(request.county)
      assertThat(countryCode).isEqualTo(request.countryCode)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(statusCode).isEqualTo(AddressStatusCode.fromPrison(request.isPrimary, request.isMail ?: false))
      assertThat(createDateTime).isEqualTo(request.createDateTime)
      assertThat(createUserId).isEqualTo(request.createUserId)
      assertThat(modifyDateTime).isEqualTo(request.modifyDateTime)
      assertThat(modifyUserId).isEqualTo(request.modifyUserId)
    }

    fun assertContactMatches(request: PrisonContact, entity: ContactEntity) = with(entity) {
      assertThat(contactType).isEqualTo(request.type)
      assertThat(contactValue).isEqualTo(request.value)
      assertThat(extension).isEqualTo(request.extension)
      assertThat(modifyUserId).isEqualTo(request.modifyUserId)
      assertThat(modifyDateTime).isEqualTo(request.modifyDateTime)
      assertThat(createUserId).isEqualTo(request.createUserId)
      assertThat(createDateTime).isEqualTo(request.createDateTime)
    }
  }
}
