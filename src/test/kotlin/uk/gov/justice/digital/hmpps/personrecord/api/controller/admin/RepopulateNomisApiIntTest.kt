package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class RepopulateNomisApiIntTest : WebTestBase() {

  @Nested
  inner class MissingRecord {

    @Test
    fun `should not do anything when person record not found in list`() {
      val prisonNumber = randomPrisonNumber()
      val request = listOf(AdminReclusterRecord(NOMIS, prisonNumber))

      webTestClient.post()
        .uri(ADMIN_REPOPULATE_NOMIS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("UUID" to prisonNumber),
        times = 0,
      )
    }

    @Test
    fun `should not recluster records that have been merged`() {
      val person = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val mergedPerson = createPersonWithNewKey(createRandomPrisonPersonDetails())

      mergeRecord(mergedPerson, person)

      mergedPerson.assertHasLinkToCluster()
      mergedPerson.assertMergedTo(person)

      val request = listOf(AdminReclusterRecord(NOMIS, mergedPerson.prisonNumber!!))

      webTestClient.post()
        .uri(ADMIN_REPOPULATE_NOMIS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("UUID" to person.personKey?.toString()),
        times = 0,
      )
    }
  }

  @Nested
  inner class ErrorRecovery {

    @Test
    fun `should retry if request to prisoner-search fails`() {
      val person = createRandomPrisonPersonDetails()
      createPerson(person)
      val prisonNumber = person.prisonNumber!!
      val request = listOf(AdminReclusterRecord(NOMIS, prisonNumber))
      stub5xxResponse(url = "/prisoner/$prisonNumber", scenarioName = "retry")

      val dateOfBirth = randomDate()
      stubPrisonResponse(
        ApiResponseSetup.from(
          person.copy(dateOfBirth = dateOfBirth),
        ),
        scenarioName = "retry",
        currentScenarioState = "Next request will succeed",
      )
      stubPersonMatchUpsert(currentScenarioState = "Next request will succeed", scenario = "retry")

      webTestClient.post()
        .uri(ADMIN_REPOPULATE_NOMIS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert { assertThat(personRepository.findByPrisonNumber(prisonNumber)?.getPrimaryName()?.dateOfBirth).isEqualTo(dateOfBirth) }
    }

    @Test
    fun `should ignore 404 if request to prisoner-search fails`() {
      val person = createRandomPrisonPersonDetails()
      val notFoundPerson = createRandomPrisonPersonDetails()
      createPerson(notFoundPerson)
      createPerson(person)
      val prisonNumber = person.prisonNumber!!
      val notFoundPrisonNumber = notFoundPerson.prisonNumber!!
      val request = listOf(
        AdminReclusterRecord(NOMIS, notFoundPrisonNumber),
        AdminReclusterRecord(NOMIS, prisonNumber),
      )
      stub404Response(url = "/prisoner/$notFoundPrisonNumber")

      val dateOfBirth = randomDate()
      stubPrisonResponse(
        ApiResponseSetup.from(
          person.copy(dateOfBirth = dateOfBirth),
        ),
      )
      stubPersonMatchUpsert()

      webTestClient.post()
        .uri(ADMIN_REPOPULATE_NOMIS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert { assertThat(personRepository.findByPrisonNumber(prisonNumber)?.getPrimaryName()?.dateOfBirth).isEqualTo(dateOfBirth) }
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
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
        .uri(ADMIN_REPOPULATE_NOMIS_URL)
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      person.personKey?.assertClusterStatus(ACTIVE)

      val updatedPerson = awaitNotNull { personRepository.findByPrisonNumber(person.prisonNumber!!) }

      assertThat(updatedPerson.getPnc()).isEqualTo(PNCIdentifier.from(pnc).pncId)
    }
  }

  companion object {
    private const val ADMIN_REPOPULATE_NOMIS_URL = "/admin/repopulate-nomis"
  }
}
