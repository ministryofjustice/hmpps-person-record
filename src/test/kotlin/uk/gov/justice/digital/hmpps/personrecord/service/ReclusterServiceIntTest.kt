package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MATCH_FOUND_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_MATCH_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId

class ReclusterServiceIntTest : MessagingMultiNodeTestBase() {

  @BeforeEach
  fun beforeEach() {
    telemetryRepository.deleteAll()
  }

  @Test
  fun `should log event if cluster needs attention`() {
    val personKeyEntity = createPersonKey(status = NEEDS_ATTENTION)
    val defendantId = randomDefendantId()
    createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )
    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId))),
    )

    checkTelemetry(
      CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
  }

  @Test
  fun `should not recluster when single record that does not match`() {
    val defendantId = randomDefendantId()
    val person = createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = createPersonKey(),
    )
    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId))),
    )

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "0",
        "UUID_COUNT" to "0",
        "HIGH_CONFIDENCE_COUNT" to "0",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
    checkTelemetry(
      CPR_RECLUSTER_NO_MATCH_FOUND,
      mapOf("UUID" to person.personKey?.personId.toString()),
    )
  }

  @Test
  fun `should recluster when single record matches to one other cluster`() {
    val cro = randomCro()
    val defendantId = randomDefendantId()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    stubOneHighConfidenceMatch()

    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = cro))),
    )

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "1",
        "UUID_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
    checkTelemetry(
      CPR_RECLUSTER_MATCH_FOUND_MERGE,
      mapOf(
        "FROM_UUID" to cluster1.personId.toString(),
        "TO_UUID" to cluster2.personId.toString(),
      ),
    )

    awaitAssert {
      val sourceCluster = personKeyRepository.findByPersonId(cluster1.personId)
      assertThat(sourceCluster?.status).isEqualTo(UUIDStatusType.RECLUSTER_MERGE)
      assertThat(sourceCluster?.personEntities?.size).isEqualTo(0)
      assertThat(sourceCluster?.mergedTo).isEqualTo(cluster2.id)
    }

    awaitAssert {
      val targetCluster = personKeyRepository.findByPersonId(cluster2.personId)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(2)
    }
  }

  // TEST FAILS WHEN 'should recluster when single record matches to one other cluster' enabled
  @Test
  fun `should recluster when single record matches to one other cluster with multiple records`() {
    val cro = randomCro()
    val defendantId = randomDefendantId()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    stubXHighConfidenceMatches(3)

    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = cro))),
    )
    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "3",
        "UUID_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "3",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
    checkTelemetry(
      CPR_RECLUSTER_MATCH_FOUND_MERGE,
      mapOf(
        "FROM_UUID" to cluster1.personId.toString(),
        "TO_UUID" to cluster2.personId.toString(),
      ),
    )

    awaitAssert {
      val sourceCluster = personKeyRepository.findByPersonId(cluster1.personId)
      assertThat(sourceCluster?.status).isEqualTo(UUIDStatusType.RECLUSTER_MERGE)
      assertThat(sourceCluster?.personEntities?.size).isEqualTo(0)
      assertThat(sourceCluster?.mergedTo).isEqualTo(cluster2.id)
    }

    awaitAssert {
      val targetCluster = personKeyRepository.findByPersonId(cluster2.personId)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(4)
    }
  }

  @Test
  fun `should recluster when single record matches to one other cluster with multiple records (only matches 1)`() {
    val cro = randomCro()
    val defendantId = randomDefendantId()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = LIBRA,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = DELIUS,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = NOMIS,
      ),
      personKeyEntity = cluster2,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.788888,
        "2" to 0.788888,
      ),
    )
    stubMatchScore(matchResponse)

    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = cro))),
    )

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "3",
        "UUID_COUNT" to "1",
        "HIGH_CONFIDENCE_COUNT" to "1",
        "LOW_CONFIDENCE_COUNT" to "2",
      ),
    )
    checkTelemetry(
      CPR_RECLUSTER_MATCH_FOUND_MERGE,
      mapOf(
        "FROM_UUID" to cluster1.personId.toString(),
        "TO_UUID" to cluster2.personId.toString(),
      ),
    )

    awaitAssert {
      val sourceCluster = personKeyRepository.findByPersonId(cluster1.personId)
      assertThat(sourceCluster?.status).isEqualTo(UUIDStatusType.RECLUSTER_MERGE)
      assertThat(sourceCluster?.personEntities?.size).isEqualTo(0)
      assertThat(sourceCluster?.mergedTo).isEqualTo(cluster2.id)
    }

    awaitAssert {
      val targetCluster = personKeyRepository.findByPersonId(cluster2.personId)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(4)
    }
  }

  @Test
  fun `should recluster when single record matches to multiple clusters`() {
    val cro = randomCro()
    val defendantId = randomDefendantId()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        defendantId = defendantId,
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    val cluster3 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster3,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.9999999,
        "1" to 0.9999991,
      ),
    )
    stubMatchScore(matchResponse)

    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId, cro = cro))),
    )

    checkTelemetry(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "2",
        "UUID_COUNT" to "2",
        "HIGH_CONFIDENCE_COUNT" to "2",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )

    awaitAssert {
      val mergeSource = personKeyRepository.findByPersonId(cluster1.personId)
      assertThat(mergeSource?.status).isEqualTo(UUIDStatusType.RECLUSTER_MERGE)
      assertThat(mergeSource?.personEntities?.size).isEqualTo(0)
    }
    val sourceCluster = personKeyRepository.findByPersonId(cluster1.personId)
    val (mergedTo, unaffected) = when {
      sourceCluster?.mergedTo == cluster2.id -> Pair(cluster2, cluster3)
      else -> Pair(cluster3, cluster2)
    }

    awaitAssert {
      val targetCluster = personKeyRepository.findByPersonId(mergedTo.personId)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(2)
    }

    awaitAssert {
      val unaffectedCluster = personKeyRepository.findByPersonId(unaffected.personId)
      assertThat(unaffectedCluster?.personEntities?.size).isEqualTo(1)
    }

    checkTelemetry(
      CPR_RECLUSTER_MATCH_FOUND_MERGE,
      mapOf(
        "FROM_UUID" to cluster1.personId.toString(),
        "TO_UUID" to mergedTo.personId.toString(),
      ),
    )
  }

  @Test
  fun `should verify multiple records in cluster match to each other`() {
    val personKeyEntity = createPersonKey()
    val defendantId = randomDefendantId()
    createPerson(
      Person(
        defendantId = defendantId,
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person(
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person(
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.9999991,
      ),
    )
    stubMatchScore(matchResponse)

    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId))),
    )

    checkTelemetry(
      CPR_RECLUSTER_NO_CHANGE,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
  }

  @Test
  fun `should verify multiple records in cluster do not match to each other`() {
    val personKeyEntity = createPersonKey()
    val defendantId = randomDefendantId()
    createPerson(
      Person(
        defendantId = defendantId,
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person(
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person(
        sourceSystem = COMMON_PLATFORM,
      ),
      personKeyEntity = personKeyEntity,
    )

    // Initial check return 1 matched record and 1 unmatched record
    val initialResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.600000,
      ),
    )
    stubMatchScore(initialResponse, nextScenarioState = "notMatchedRecordCheck")

    // Then
    // No high confidence link when checking the unmatched record against the cluster
    val noMatchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.600000,
        "1" to 0.600000,
      ),
    )
    stubMatchScore(noMatchResponse, currentScenarioState = "notMatchedRecordCheck")

    stubPersonMatchUpsert()
    publishCommonPlatformMessage(
      commonPlatformHearing(listOf(CommonPlatformHearingSetup(defendantId = defendantId))),
    )

    checkTelemetry(
      CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )

    awaitAssert {
      val cluster = personKeyRepository.findByPersonId(personKeyEntity.personId)
      assertThat(cluster?.status).isEqualTo(NEEDS_ATTENTION)
    }
  }
}
