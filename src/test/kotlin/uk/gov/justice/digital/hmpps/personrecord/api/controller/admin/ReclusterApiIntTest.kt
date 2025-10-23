package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_ADMIN_RECLUSTER_TRIGGERED
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ReclusterApiIntTest : WebTestBase() {

  @Nested
  inner class MissingRecord {

    @Test
    fun `should not do anything when person record not found in list`() {
      val defendantId = randomDefendantId()
      val request = listOf(AdminReclusterRecord(SourceSystemType.COMMON_PLATFORM, defendantId))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to defendantId),
        times = 0,
      )
    }

    @Test
    fun `should not recluster records that have been merged`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val mergedPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      mergeRecord(mergedPerson, person)

      mergedPerson.assertHasLinkToCluster()
      mergedPerson.assertMergedTo(person)

      val request = listOf(AdminReclusterRecord(DELIUS, mergedPerson.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.toString()),
        times = 0,
      )
    }
  }

  @Nested
  inner class ErrorRecovery {

    @Disabled
    @Test
    fun `should retry if request to hmpps-person-match fails`() {
      stubPersonMatchUpsert()
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val request = listOf(AdminReclusterRecord(DELIUS, person.crn!!))
      stub5xxResponse(url = "/person/score/" + person.matchId)
      stubPersonMatchScores(
        matchId = person.matchId,
        personMatchResponse = listOf(),
        currentScenarioState = "Next request will succeed",
      )

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk
      person.personKey?.assertClusterStatus(ACTIVE)
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
    }

    @Disabled
    @Test
    fun `should recluster single person record`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val request = listOf(AdminReclusterRecord(DELIUS, person.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
    }

    @Disabled
    @Test
    fun `should recluster multiple person records`() {
      val recordsWithCluster = List(5) {
        createPersonWithNewKey(createRandomProbationPersonDetails())
      }
      val request = recordsWithCluster.map { AdminReclusterRecord(it.sourceSystem, it.crn!!) }

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      recordsWithCluster.forEach {
        checkTelemetry(
          CPR_ADMIN_RECLUSTER_TRIGGERED,
          mapOf("UUID" to it.personKey?.personUUID.toString()),
        )
      }
    }

    @Test
    fun `should set PNC on prison record before recluster`() {
      val pnc = randomShortPnc()

      val prisonNumber = randomPrisonNumber()
      val createPerson = createRandomPrisonPersonDetails(prisonNumber = prisonNumber)

      val person = createPersonWithNewKey(createPerson, status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val request = listOf(AdminReclusterRecord(NOMIS, person.prisonNumber!!))

      stubPrisonResponse(
        ApiResponseSetup.from(
          createPerson.copy(
            contacts = listOf(Contact(ContactType.EMAIL, randomEmail())),
            references = listOf(Reference(IdentifierType.PNC, pnc)),
          ),
        ),
      )

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      person.personKey?.assertClusterStatus(ACTIVE)

      val updatedPerson = awaitNotNull { personRepository.findByPrisonNumber(person.prisonNumber!!) }

      assertThat(updatedPerson.getPnc()).isEqualTo(PNCIdentifier.from(pnc).pncId)
    }

    @Disabled
    @Test
    fun `should set needs attention to active when cluster is valid`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = NEEDS_ATTENTION, reason = BROKEN_CLUSTER)
      val request = listOf(AdminReclusterRecord(DELIUS, person.crn!!))

      webTestClient.post()
        .uri(ADMIN_RECLUSTER_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_ADMIN_RECLUSTER_TRIGGERED,
        mapOf("UUID" to person.personKey?.personUUID.toString()),
      )
      person.personKey?.assertClusterStatus(ACTIVE)
    }
  }

  companion object {
    private const val ADMIN_RECLUSTER_URL = "/admin/recluster"
  }
}
