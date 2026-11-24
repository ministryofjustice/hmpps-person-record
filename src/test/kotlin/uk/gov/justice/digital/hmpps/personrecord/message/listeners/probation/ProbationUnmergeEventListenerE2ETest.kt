package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PROBATION_PERSON_DELETED_GDPR
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OverrideScopeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationUnmergeEventListenerE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var overrideScopeRepository: OverrideScopeRepository

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `check the status of the override marker and scope after a record is unmerged`() {
      val remainingCrn = randomCrn()
      val deletedCrn = randomCrn()

      val deleted = createPersonWithNewKey(createRandomProbationPersonDetails(deletedCrn))
      val remainingPersonData = createRandomProbationCase(remainingCrn)
      val remainingPerson = Person.from(remainingPersonData)
      val remaining = createPersonWithNewKey(remainingPerson)

      probationMergeEventAndResponseSetup(remainingCrn, deletedCrn)

      checkEventLogExist(remainingCrn, CPRLogEvents.CPR_RECORD_MERGED)
      remaining.assertMergedTo(deleted)

      val remainingSetup = ApiResponseSetup.from(remainingPersonData)
      probationUnmergeEventAndResponseSetup(remainingCrn, deletedCrn, reactivatedSetup = remainingSetup)

      checkEventLogExist(remainingCrn, CPRLogEvents.CPR_UUID_CREATED)

      awaitAssert {
        remaining.assertExcluded(personRepository.findByCrn(deletedCrn)!!)
      }

      val deletedScope = personRepository.findByCrn(deletedCrn)?.overrideScopes
      publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETED_GDPR, deletedCrn)

      checkEventLogExist(deletedCrn, CPRLogEvents.CPR_RECORD_DELETED)

      awaitAssert {
        val remainAfterDelete = personRepository.findByCrn(remainingCrn)!!
        assertThat(remainAfterDelete.overrideMarker).isNotNull()
        assertThat(remainAfterDelete.overrideScopes.first().scope).isEqualTo(deletedScope?.first()?.scope) // the scope is still present on the remaining record
        assertThat(overrideScopeRepository.findById(deletedScope?.first()?.id!!)).isNotNull() // the override scope of the deleted record is still there
      }
    }

    @Test
    fun `should unmerge 2 merged records that exist on same cluster and then link to another cluster`() {
      val recordACrn = randomCrn()
      val recordBCrn = randomCrn()
      val recordADetails = createRandomProbationCase(recordACrn)
      val cluster = createPersonKey()
        .addPerson(Person.from(recordADetails))
        .addPerson(createRandomProbationPersonDetails(recordBCrn))

      probationMergeEventAndResponseSetup(recordACrn, recordBCrn)

      val recordA = personRepository.findByCrn(recordACrn)!!
      val recordB = personRepository.findByCrn(recordBCrn)!!
      recordA.assertMergedTo(recordB)

      val matchedRecord = createPersonWithNewKey(Person.from(recordADetails).copy(crn = randomCrn()))

      probationUnmergeEventAndResponseSetup(recordACrn, recordBCrn, reactivatedSetup = ApiResponseSetup.from(recordADetails))

      checkEventLogExist(recordACrn, CPR_RECORD_UNMERGED)
      recordB.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      recordA.assertNotMerged()

      recordA.assertExcluded(recordB)
      recordB.assertExcluded(recordA)

      cluster.assertClusterStatus(UUIDStatusType.ACTIVE)
      cluster.assertClusterIsOfSize(1)

      recordB.assertLinkedToCluster(cluster)
      recordA.assertLinkedToCluster(matchedRecord.personKey!!)

      matchedRecord.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      matchedRecord.personKey?.assertClusterIsOfSize(2)

      matchedRecord.assertDoesNotHaveOverrideMarker()
      recordB.assertHasOverrideMarker()
      recordA.assertHasOverrideMarker()

      recordB.assertHasDifferentOverrideMarker(recordA)
      recordB.assertHasSameOverrideScope(recordA)
    }
  }

  @Nested
  inner class NeedsAttention {

    @Test
    fun `should mark unmerged UUID as needs attention if it has additional records`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val unmergedPersonDetails = createRandomProbationCase(unmergedCrn)
      val reactivatedPersonDetails = createRandomProbationCase(reactivatedCrn)

      val unmergedSetup = ApiResponseSetup.from(unmergedPersonDetails)
      val reactivatedPerson = createPersonWithNewKey(Person.from(reactivatedPersonDetails))
      val cluster = createPersonKey()
        .addPerson(Person.from(unmergedPersonDetails))
        .addPerson(createMatchingRecord(unmergedPersonDetails))
      val unmergedPerson = cluster.personEntities.first()

      probationMergeEventAndResponseSetup(reactivatedCrn, unmergedCrn, apiResponseSetup = unmergedSetup)

      checkEventLogExist(reactivatedCrn, CPRLogEvents.CPR_RECORD_MERGED)
      reactivatedPerson.assertMergedTo(unmergedPerson)

      probationUnmergeEventAndResponseSetup(
        reactivatedCrn,
        unmergedCrn,
        reactivatedSetup = ApiResponseSetup.from(reactivatedPersonDetails),
        unmergedSetup = unmergedSetup,
      )

      checkEventLogExist(reactivatedCrn, CPR_RECORD_UNMERGED)

      cluster.assertClusterStatus(UUIDStatusType.NEEDS_ATTENTION, reason = UUIDStatusReasonType.OVERRIDE_CONFLICT)
      cluster.assertClusterIsOfSize(2)

      unmergedPerson.assertLinkedToCluster(cluster)
      unmergedPerson.assertExcluded(reactivatedPerson)

      reactivatedPerson.assertNotMerged()
      reactivatedPerson.assertHasLinkToCluster()
      reactivatedPerson.assertNotLinkedToCluster(cluster)
      reactivatedPerson.assertExcluded(unmergedPerson)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPerson.assertHasOverrideMarker()
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPerson)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPerson)

      val reactivatedPersonKey = awaitNotNull { personRepository.findByCrn(reactivatedCrn) }.personKey!!
      cluster.getReviews().first()
        .assertReviewSize(2)
        .isPrimary(cluster)
        .isAdditional(reactivatedPersonKey)
    }
  }
}
