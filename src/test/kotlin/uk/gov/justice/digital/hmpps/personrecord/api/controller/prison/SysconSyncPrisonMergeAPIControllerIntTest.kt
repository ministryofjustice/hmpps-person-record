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
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import kotlin.jvm.optionals.getOrNull

class SysconSyncPrisonMergeAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class MissingFromRecord {

    @Test
    fun `processes prisoner merge event when source record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      stubPersonMatchUpsert()
      prisonMergeEventAndResponseSetup(sourcePrisonNumber = sourcePrisonNumber, targetPrisonNumber = targetPrisonNumber)

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("PRISON_NUMBER" to targetPrisonNumber))
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class MissingToRecord {

    @Test
    fun `processes prisoner merge event when target record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))

      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      val targetPerson = awaitNotNull { personRepository.findByPrisonNumber(targetPrisonNumber) }

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_CREATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event with records with same UUID is published`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      createPersonKey()
        .addPerson(sourcePerson)
        .addPerson(targetPerson)

      prisonMergeEventAndResponseSetup(sourcePrisonNumber = sourcePrisonNumber, targetPrisonNumber = targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
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
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has multiple records`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val sourceCluster = createPersonKey()
        .addPerson(Person(prisonNumber = randomPrisonNumber(), sourceSystem = NOMIS))
        .addPerson(sourcePerson)
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      sourceCluster.assertClusterIsOfSize(1)
      sourceCluster.assertClusterStatus(UUIDStatusType.ACTIVE)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source doesn't have an UUID`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("PRISON_NUMBER" to targetPrisonNumber, "SOURCE_SYSTEM" to NOMIS.name),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has a single record`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      val sourceClusterId = sourcePerson.personKey?.id

      prisonMergeEventAndResponseSetup(sourcePrisonNumber, targetPrisonNumber)

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
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLog(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
        assertThat(eventLogs.first().personUUID).isEqualTo(sourcePerson.personKey!!.personUUID)
      }
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `should fail with a 500 error when message processing fails`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure")

      publishPrisonPersonMergedEvent(sourcePrisonNumber, targetPrisonNumber, HttpStatus.INTERNAL_SERVER_ERROR)
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
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }

      prisonMergeEventAndResponseSetup(
        sourcePrisonNumber = sourcePrisonNumber,
        targetPrisonNumber = targetPrisonNumber,
      )

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
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }

    @Test
    fun `processes prisoner merge event and source record passive state is not maintained on target record`() {
      stubPersonMatchUpsert()

      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(
        sourcePrisonNumber = sourcePrisonNumber,
        targetPrisonNumber = targetPrisonNumber,
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
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
    }
  }

  @Nested
  inner class Authorisation {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = PrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = PrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonMergeEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/merge"
  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  private fun prisonMergeEventAndResponseSetup(
    sourcePrisonNumber: String,
    targetPrisonNumber: String,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
    expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
  ) {
    stubPrisonResponse(
      ApiResponseSetup(prisonNumber = targetPrisonNumber),
      scenario,
      currentScenarioState,
      nextScenarioState,
    )
    publishPrisonPersonMergedEvent(sourcePrisonNumber, targetPrisonNumber, expectedStatus)
  }
  private fun publishPrisonPersonMergedEvent(
    sourcePrisonNumber: String,
    targetPrisonNumber: String,
    expectedStatus: HttpStatus = HttpStatus.NO_CONTENT,
  ) {
    sendPostRequestAsserted<Unit>(
      url = prisonMergeEndpoint(targetPrisonNumber),
      body = PrisonMerge(fromPrisonNumber = sourcePrisonNumber),
      roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
      expectedStatus = expectedStatus,
    )
  }
}
