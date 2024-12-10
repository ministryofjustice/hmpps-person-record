package uk.gov.justice.digital.hmpps.personrecord.message.listeners.cpr

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.MatchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.CPR
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.queue.QueueService
import uk.gov.justice.digital.hmpps.personrecord.service.type.RECLUSTER_EVENT
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MATCH_FOUND_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_MATCH_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.time.LocalDateTime

class ReclusterEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  private lateinit var queueService: QueueService

  @Test
  fun `should log event if cluster needs attention`() {
    val personKeyEntity = createPersonKey(status = NEEDS_ATTENTION)
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )

    queueService.publishReclusterMessageToQueue(personKeyEntity.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
    checkTelemetry(
      CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
  }

  @Test
  fun `should not recluster when single record that does not match`() {
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    queueService.publishReclusterMessageToQueue(cluster1.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to cluster1.personId.toString()),
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
      mapOf("UUID" to cluster1.personId.toString()),
    )
  }

  @Test
  fun `should recluster when single record matches to one other cluster`() {
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )
    val beforeData = objectMapper.writeValueAsString(personKeyRepository.findByPersonId(cluster1.personId))

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
      ),
    )
    stubMatchScore(matchResponse)

    queueService.publishReclusterMessageToQueue(cluster1.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to cluster1.personId.toString()),
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

    val afterData = objectMapper.writeValueAsString(personKeyRepository.findByPersonId(cluster1.personId))

    val loggedEvent = awaitNotNullEventLog(cluster1.personId!!)

    assertThat(loggedEvent.eventType).isEqualTo(RECLUSTER_EVENT)
    assertThat(loggedEvent.sourceSystemId).isEqualTo(null)
    assertThat(loggedEvent.sourceSystem).isEqualTo(CPR.name)
    assertThat(loggedEvent.eventTimestamp).isBefore(LocalDateTime.now())
    assertThat(loggedEvent.beforeData).isEqualTo(beforeData)
    assertThat(loggedEvent.processedData).isEqualTo(afterData)
    assertThat(loggedEvent.uuid).isEqualTo(cluster1.personId.toString())
  }

  @Test
  fun `should recluster when single record matches to one other cluster with multiple records`() {
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.999999,
        "2" to 0.999999,
      ),
    )
    stubMatchScore(matchResponse)

    queueService.publishReclusterMessageToQueue(cluster1.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to cluster1.personId.toString()),
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
  }

  @Test
  fun `should recluster when single record matches to one other cluster with multiple records (only matches 1)`() {
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = LIBRA,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = DELIUS,
      ),
      personKeyEntity = cluster2,
    )
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = NOMIS,
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

    queueService.publishReclusterMessageToQueue(cluster1.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to cluster1.personId.toString()),
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
  }

  @Test
  fun `should recluster when single record matches to multiple clusters`() {
    val cro = randomCro()
    val cluster1 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster1,
    )

    val cluster2 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
      ),
      personKeyEntity = cluster2,
    )

    val cluster3 = createPersonKey()
    createPerson(
      Person(
        references = listOf(Reference(IdentifierType.CRO, cro)),
        sourceSystemType = COMMON_PLATFORM,
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

    queueService.publishReclusterMessageToQueue(cluster1.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to cluster1.personId.toString()),
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
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        "SOURCE_SYSTEM" to COMMON_PLATFORM.name,
        "RECORD_COUNT" to "2",
        "UUID_COUNT" to "2",
        "HIGH_CONFIDENCE_COUNT" to "2",
        "LOW_CONFIDENCE_COUNT" to "0",
      ),
    )
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
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )

    val matchResponse = MatchResponse(
      matchProbabilities = mutableMapOf(
        "0" to 0.999999,
        "1" to 0.9999991,
      ),
    )
    stubMatchScore(matchResponse)

    queueService.publishReclusterMessageToQueue(personKeyEntity.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
    checkTelemetry(
      CPR_RECLUSTER_NO_CHANGE,
      mapOf("UUID" to personKeyEntity.personId.toString()),
    )
  }

  @Test
  fun `should verify multiple records in cluster do not match to each other`() {
    val personKeyEntity = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
      personKeyEntity = personKeyEntity,
    )
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCRN()))),
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

    queueService.publishReclusterMessageToQueue(personKeyEntity.personId!!)

    checkTelemetry(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf("UUID" to personKeyEntity.personId.toString()),
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
