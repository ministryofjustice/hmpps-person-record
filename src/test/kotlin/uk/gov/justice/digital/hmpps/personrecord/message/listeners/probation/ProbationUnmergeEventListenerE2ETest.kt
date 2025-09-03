package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
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

      val cluster = createPersonKey()
      val unmergedPerson = createPerson(createRandomProbationPersonDetails(unmergedCrn), cluster)
      val reactivatedPerson = createRandomProbationPersonDetails(reactivatedCrn)
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
        assertThat(eventLog.excludeOverrideMarkers).contains(unmergedPerson.id)
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
      unmergedPerson.assertExcludedFrom(reactivatedPersonEntity)

      reactivatedPersonEntity.assertHasLinkToCluster()
      reactivatedPersonEntity.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      reactivatedPersonEntity.personKey?.assertClusterIsOfSize(1)
      reactivatedPersonEntity.assertNotLinkedToCluster(unmergedPerson.personKey!!)
      reactivatedPersonEntity.assertExcludedFrom(unmergedPerson)
      reactivatedPersonEntity.assertNotMerged()

      unmergedPerson.assertHasOverrideMarker()
      reactivatedPersonEntity.assertHasOverrideMarker()
      unmergedPerson.assertOverrideScopeSize(1)
      reactivatedPersonEntity.assertOverrideScopeSize(1)
      unmergedPerson.assertHasDifferentOverrideMarker(reactivatedPersonEntity)
      unmergedPerson.assertHasSameOverrideScope(reactivatedPersonEntity)
    }
  }
}
