package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class RemandOverrideMarkersE2ETest : E2ETestBase() {

  @Test
  fun `should create record to match and join an existing cluster with include override markers`() {
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
    val updatedDefendant = personRepository.findByMatchId(defendantRecord.matchId)!!

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)

    val matchingDefendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = defendant.getPnc(), lastName = defendant.lastName!!, cro = defendant.getCro()!!, defendantId = matchingDefendantId, masterDefendantId = defendant.masterDefendantId!!))),
    )

    val matchingPerson = awaitNotNull { personRepository.findByDefendantId(matchingDefendantId) }
    matchingPerson.assertLinkedToCluster(updatedDefendant.personKey!!)
  }

  @Test
  @Disabled("clusters with more than one record on which have a record unmerged out of them automatically go into needs attention")
  fun `should update a record to match and join an existing cluster with include and exclude override markers`() {
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

    // create excluded cluster
    val excludedCrnOnDifferentCluster = randomCrn()
    createPersonWithNewKey(Person.from(createRandomProbationCase(excludedCrnOnDifferentCluster)))

    probationMergeEventAndResponseSetup(excludedCrnOnDifferentCluster, offender.crn!!)

    checkEventLogExist(excludedCrnOnDifferentCluster, CPRLogEvents.CPR_RECORD_MERGED)

    val excludedCrnSetup = ApiResponseSetup.from(createRandomProbationCase(excludedCrnOnDifferentCluster))
    probationUnmergeEventAndResponseSetup(excludedCrnOnDifferentCluster, offender.crn!!, reactivatedSetup = excludedCrnSetup)

    checkEventLogExist(excludedCrnOnDifferentCluster, CPRLogEvents.CPR_UUID_CREATED)

    // create a new probation record with different CRN but same person details
    val probationPersonCrn = randomCrn()
    val probationCaseToCreate = createRandomProbationCase().copy(identifiers = Identifiers(crn = probationPersonCrn))

    probationCreateEventAndResponseSetup(ApiResponseSetup.from(probationCaseToCreate))

    val createdProbationPerson = awaitNotNull { personRepository.findByCrn(probationPersonCrn) }
    createdProbationPerson.assertNotLinkedToCluster(updatedDefendant.personKey!!)

    val updateProbationPerson = probationCase.copy(identifiers = Identifiers(crn = probationPersonCrn, cro = defendant.getCro(), pnc = defendant.getPnc()))
    probationUpdateEventAndResponseSetup(ApiResponseSetup.from(updateProbationPerson))

    updatedDefendant.assertLinkedToCluster(createdProbationPerson.personKey!!)
  }

  @Test
  fun `should update a record to match and join an existing cluster with include override markers`() {
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
    val updatedDefendant = personRepository.findByMatchId(defendantRecord.matchId)!!

    offender.assertLinkedToCluster(updatedDefendant.personKey!!)
    val matchingDefendantId = randomDefendantId()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = matchingDefendantId, masterDefendantId = defendant.masterDefendantId!!))),
    )
    val newCourtRecord = awaitNotNull { personRepository.findByDefendantId(matchingDefendantId) }
    newCourtRecord.assertNotLinkedToCluster(updatedDefendant.personKey!!)
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(pnc = defendant.getPnc(), lastName = defendant.lastName!!, cro = defendant.getCro()!!, defendantId = matchingDefendantId, masterDefendantId = defendant.masterDefendantId!!))),
    )

    updatedDefendant.assertLinkedToCluster(newCourtRecord.personKey!!)
    newCourtRecord.personKey!!.assertClusterIsOfSize(3)
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
  @Disabled("Not implemented yet")
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
  @Disabled("clusters with more than one record on which have a record unmerged out of them automatically go into needs attention")
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
