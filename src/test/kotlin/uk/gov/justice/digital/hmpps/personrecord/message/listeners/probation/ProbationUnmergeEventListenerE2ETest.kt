package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias

class ProbationUnmergeEventListenerE2ETest : E2ETestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should unmerge 2 records that have been merged`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val unmergedPerson = createPerson(createRandomProbationPersonDetails(unmergedCrn))
      val cluster = createPersonKey().addPerson(unmergedPerson)
      val reactivatedPerson = createRandomProbationPersonDetails(reactivatedCrn)
      val masterDefendantId = randomDefendantId()
      reactivatedPerson.masterDefendantId = masterDefendantId
      val reactivatedPersonEntity = createPerson(reactivatedPerson)
      val reactivatedSetup = ApiResponseSetup(
        crn = reactivatedCrn,
        cro = reactivatedPerson.getCro(),
        pnc = reactivatedPerson.getPnc(),
        firstName = reactivatedPerson.firstName,
        middleName = reactivatedPerson.middleNames,
        lastName = reactivatedPerson.lastName,
        dateOfBirth = reactivatedPerson.dateOfBirth,
        addresses = listOf(ApiResponseSetupAddress(postcode = reactivatedPerson.addresses.first().postcode, fullAddress = randomFullAddress())),
        aliases = listOf(ApiResponseSetupAlias(firstName = reactivatedPerson.aliases.first().firstName!!, middleName = reactivatedPerson.aliases.first().middleNames!!, lastName = reactivatedPerson.aliases.first().lastName!!, dateOfBirth = reactivatedPerson.aliases.first().dateOfBirth!!)),
      )
      probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivatedCrn, unmergedCrn)

      checkEventLogExist(reactivatedCrn, CPRLogEvents.CPR_RECORD_MERGED)
      reactivatedPersonEntity.assertMergedTo(unmergedPerson)

      probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, reactivatedCrn, unmergedCrn, reactivatedSetup = reactivatedSetup)

      checkEventLogExist(reactivatedCrn, CPRLogEvents.CPR_UUID_CREATED)
      checkEventLog(reactivatedCrn, CPRLogEvents.CPR_RECORD_UNMERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        val eventLog = eventLogs.first()
        assertThat(eventLog.personUUID).isNotEqualTo(cluster.personUUID)
        assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }

      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "UNMERGED_UUID" to cluster.personUUID.toString(),
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      unmergedPerson.assertHasLinkToCluster()
      unmergedPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      unmergedPerson.personKey?.assertClusterIsOfSize(1)
      unmergedPerson.assertExcluded(reactivatedPersonEntity)

      reactivatedPersonEntity.assertHasLinkToCluster()
      reactivatedPersonEntity.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      reactivatedPersonEntity.personKey?.assertClusterIsOfSize(1)
      reactivatedPersonEntity.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPersonEntity.assertExcluded(unmergedPerson)
      reactivatedPersonEntity.assertNotMerged()
      val updatedReactivatedPersonEntity = awaitNotNull { personRepository.findByCrn(reactivatedCrn) }
      assertThat(updatedReactivatedPersonEntity.masterDefendantId).isEqualTo(masterDefendantId)

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPersonEntity.assertHasOverrideMarker()
      unmergedPerson.assertOverrideScopeSize(1)
      reactivatedPersonEntity.assertOverrideScopeSize(1)
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPersonEntity)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPersonEntity)
    }
  }

  @Nested
  inner class NeedsAttention {

    @Test
    fun `should mark unmerged UUID as needs attention if it has additional records`() {
      val reactivatedCrn = randomCrn()
      val unmergedCrn = randomCrn()

      val basePersonData = createRandomProbationPersonDetails()

      val reactivatedPersonData = createProbationPersonFrom(basePersonData, reactivatedCrn)
      val unmergedPersonData = createProbationPersonFrom(basePersonData, unmergedCrn)
      val reactivatedPerson = createPersonWithNewKey(reactivatedPersonData)
      val unmergedPerson = createPerson(unmergedPersonData)
      val cluster = createPersonKey()
        .addPerson(unmergedPerson)
        .addPerson(createProbationPersonFrom(basePersonData))

      probationMergeEventAndResponseSetup(OFFENDER_MERGED, reactivatedCrn, unmergedCrn, apiResponseSetup = ApiResponseSetup.from(unmergedPersonData))

      checkEventLogExist(reactivatedCrn, CPRLogEvents.CPR_RECORD_MERGED)
      reactivatedPerson.assertMergedTo(unmergedPerson)

      probationUnmergeEventAndResponseSetup(
        OFFENDER_UNMERGED,
        reactivatedCrn,
        unmergedCrn,
        reactivatedSetup = ApiResponseSetup.from(reactivatedPersonData),
        unmergedSetup = ApiResponseSetup.from(unmergedPersonData),
      )

      checkTelemetry(
        CPR_RECORD_UNMERGED,
        mapOf(
          "TO_SOURCE_SYSTEM_ID" to reactivatedCrn,
          "FROM_SOURCE_SYSTEM_ID" to unmergedCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

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
    }
  }
}
