package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class RemandOverrideMarkersE2ETest : E2ETestBase() {

  @BeforeEach
  fun setup() {
    deleteAllPersonData() // temporary
  }

  @Test
  fun `should create link between probation and court records and assign to same cluster, override marker and override scope`() {
    val defendantId = randomDefendantId()
    val defendant = createRandomCommonPlatformPersonDetails(defendantId)
    val probationCase = ProbationCase(
      name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
      identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
      dateOfBirth = defendant.dateOfBirth,
    )
    val defendantRecord = createPersonWithNewKey(defendant)

    webTestClient.put()
      .uri(probationApiUrl(defendantId))
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

    val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }
    val updatedDefendant = awaitNotNull { personRepository.findByMatchId(defendantRecord.matchId) }

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)
    offender.assertHasSameOverrideMarker(updatedDefendant)
    offender.assertHasSameOverrideScope(updatedDefendant)
  }

  @Test
  fun `should create new probation record and join existing cluster when created with the same person details`() {
    val defendantId = randomDefendantId()
    val defendant = createRandomCommonPlatformPersonDetails(defendantId)
    val probationCase = ProbationCase(
      name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
      identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
      dateOfBirth = defendant.dateOfBirth,
    )
    val defendantRecord = createPersonWithNewKey(defendant)

    webTestClient.put()
      .uri(probationApiUrl(defendantId))
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

    val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }
    val updatedDefendant = awaitNotNull { personRepository.findByMatchId(defendantRecord.matchId) }

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)

    val similarPersonCrn = randomCrn()
    val similarPersonDetails = probationCase.copy(identifiers = Identifiers(crn = similarPersonCrn, cro = defendant.getCro(), pnc = defendant.getPnc()))

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(similarPersonDetails))
    val similarPerson = awaitNotNull { personRepository.findByCrn(similarPersonCrn) }
    similarPerson.assertLinkedToCluster(updatedDefendant.personKey!!)
    // RESULT: on a separate cluster in NEEDS_ATTENTION - fails because PersonKeyService.containsExcluded returns true and a new cluster is created instead of joining the existing cluster
  }

  @Test
  fun `should create new probation record on separate cluster when created with different person details`() {
    val defendantId = randomDefendantId()
    val defendant = createRandomCommonPlatformPersonDetails(defendantId)
    val probationCase = ProbationCase(
      name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
      identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
      dateOfBirth = defendant.dateOfBirth,
    )
    val defendantRecord = createPersonWithNewKey(defendant)

    webTestClient.put()
      .uri(probationApiUrl(defendantId))
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

    val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }
    val updatedDefendant = awaitNotNull { personRepository.findByMatchId(defendantRecord.matchId) }

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)

    val similarCrn = randomCrn()
    val similarPersonDetails = createRandomProbationCase(similarCrn)

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(similarPersonDetails))
    val similarPerson = awaitNotNull { personRepository.findByCrn(similarCrn) }
    similarPerson.assertNotLinkedToCluster(offender.personKey!!)
    similarPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
  }

  @Test
  fun `should merge remand probation record into new probation record and maintain the include override marker`() {
    val defendantId = randomDefendantId()
    val defendant = createRandomCommonPlatformPersonDetails(defendantId)
    val probationCase = ProbationCase(
      name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
      identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
      dateOfBirth = defendant.dateOfBirth,
    )
    val defendantRecord = createPersonWithNewKey(defendant)

    webTestClient.put()
      .uri(probationApiUrl(defendantId))
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

    val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }
    val updatedDefendant = awaitNotNull { personRepository.findByMatchId(defendantRecord.matchId) }

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)

    val targetCrn = randomCrn()
    val targetPersonDetails = createRandomProbationCase(targetCrn)

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(targetPersonDetails))
    val targetPerson = awaitNotNull { personRepository.findByCrn(targetCrn) }
    targetPerson.assertNotLinkedToCluster(offender.personKey!!)

    probationMergeEventAndResponseSetup(
      sourceCrn = offender.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )

    offender.assertMergedTo(targetPerson)
    targetPerson.assertHasSameOverrideMarker(defendantRecord)
    // RESULT: fails because the override marker is not maintained after merge
  }

  @Test
  fun `should merge new probation record into remand probation record and maintain the include override marker`() {
    val defendantId = randomDefendantId()
    val defendant = createRandomCommonPlatformPersonDetails(defendantId)
    val probationCase = ProbationCase(
      name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
      identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
      dateOfBirth = defendant.dateOfBirth,
    )
    val defendantRecord = createPersonWithNewKey(defendant)

    webTestClient.put()
      .uri(probationApiUrl(defendantId))
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

    val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }
    val updatedDefendant = awaitNotNull { personRepository.findByMatchId(defendantRecord.matchId) }

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)

    val sourceCrn = randomCrn()
    val sourcePersonDetails = createRandomProbationCase(sourceCrn)

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(sourcePersonDetails))
    val sourcePerson = awaitNotNull { personRepository.findByCrn(sourceCrn) }
    sourcePerson.assertNotLinkedToCluster(offender.personKey!!)

    probationMergeEventAndResponseSetup(
      sourceCrn = sourcePerson.crn!!,
      targetCrn = offender.crn!!,
      apiResponseSetup = ApiResponseSetup.from(probationCase),
    )

    sourcePerson.assertMergedTo(offender)
    offender.assertHasSameOverrideMarker(updatedDefendant)
    offender.assertHasSameOverrideScope(updatedDefendant)
  }

  @Test
  fun `should merge remand probation record and then unmerge`() {
    val defendantId = randomDefendantId()
    val defendant = createRandomCommonPlatformPersonDetails(defendantId)
    val probationCase = ProbationCase(
      name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
      identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
      dateOfBirth = defendant.dateOfBirth,
    )
    val defendantRecord = createPersonWithNewKey(defendant)

    webTestClient.put()
      .uri(probationApiUrl(defendantId))
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

    val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }
    val updatedDefendant = awaitNotNull { personRepository.findByMatchId(defendantRecord.matchId) }

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)

    val targetCrn = randomCrn()
    val targetPersonDetails = createRandomProbationCase(targetCrn)

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(targetPersonDetails))
    val targetPerson = awaitNotNull { personRepository.findByCrn(targetCrn) }
    targetPerson.assertNotLinkedToCluster(offender.personKey!!)

    probationMergeEventAndResponseSetup(
      sourceCrn = offender.crn!!,
      targetCrn = targetPerson.crn!!,
      apiResponseSetup = ApiResponseSetup.from(targetPersonDetails),
    )

    offender.assertMergedTo(targetPerson)
//    targetPerson.assertHasSameOverrideMarker(defendantRecord)
    // the above assertion fails because the override marker is not maintained after merge

    probationUnmergeEventAndResponseSetup(
      offender.crn!!,
      targetCrn,
      reactivatedSetup = ApiResponseSetup.from(probationCase).copy(crn = offender.crn!!),
      unmergedSetup = ApiResponseSetup.from(targetPersonDetails).copy(crn = targetPerson.crn!!),
    )

    offender.assertNotMerged()
    defendantRecord.personKey?.assertClusterIsOfSize(2)
    // RESULT: fails because PersonMatchService.removeExcludedClusters is filtering out all markers with scopes
  }

  // merge -> unmerge -> create remand -> merge

  private fun probationApiUrl(defendantId: String) = "/person/probation/$defendantId"
}
