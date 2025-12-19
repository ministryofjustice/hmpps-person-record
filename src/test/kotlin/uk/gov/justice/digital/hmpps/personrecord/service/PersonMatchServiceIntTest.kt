package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH

class PersonMatchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personMatchService: PersonMatchService

  @Nested
  inner class IsClusterValid {

    @Test
    fun `should process isClusterValid response`() {
      val personA = createPerson(createRandomLibraPersonDetails())
      val personB = createPerson(createRandomLibraPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubPostRequest(
        url = "/is-cluster-valid",
        status = 200,
        responseBody = """
          {
            "isClusterValid": true,
            "clusters": [["${personA.matchId}", "${personB.matchId}"]]
          }
        """.trimIndent(),
      )

      personMatchService.examineIsClusterValid(cluster)
    }

    @Test
    fun `should handle out of sync isClusterValid response`() {
      val personA = createPerson(createRandomLibraPersonDetails())
      val personB = createPerson(createRandomLibraPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubPostRequest(
        url = "/is-cluster-valid",
        status = 404,
        responseBody = """
          {
            "unknownIds": ["${personA.matchId}"]
          }
        """.trimIndent(),
        nextScenarioState = "FOUND ALL RECORDS",
      )
      stubPersonMatchUpsert(currentScenarioState = "FOUND ALL RECORDS", nextScenarioState = "CLUSTER IS VALID")
      stubClusterIsValid(
        currentScenarioState = "CLUSTER IS VALID",
      )

      val result = personMatchService.examineIsClusterValid(cluster)
      assertThat(result.isClusterValid).isTrue()
    }

    @Test
    fun `should handle out of sync isClusterMergeValid response`() {
      val personA = createPerson(createRandomLibraPersonDetails())
      val personB = createPerson(createRandomLibraPersonDetails())
      val cluster1 = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      val personC = createPerson(createRandomLibraPersonDetails())
      val personD = createPerson(createRandomLibraPersonDetails())
      val cluster2 = createPersonKey()
        .addPerson(personC)
        .addPerson(personD)

      stubPostRequest(
        url = "/is-cluster-valid",
        status = 404,
        responseBody = """
          {
            "unknownIds": ["${personC.matchId}"]
          }
        """.trimIndent(),
        nextScenarioState = "FOUND ALL RECORDS",
      )
      stubPersonMatchUpsert(currentScenarioState = "FOUND ALL RECORDS", nextScenarioState = "CLUSTER IS VALID")
      stubClusterIsValid(
        currentScenarioState = "CLUSTER IS VALID",
      )

      val result = personMatchService.examineIsClusterMergeValid(cluster1, listOf(cluster2))
      assertThat(result.isClusterValid).isTrue()
    }
  }

  @Nested
  inner class Scoring {

    @Test
    fun `should find one high confidence match for record not assigned to cluster`() {
      val searchingRecord = createPerson(createRandomLibraPersonDetails())
      val foundRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)
      assertThat(highConfidenceMatch.first().personUUID).isEqualTo(foundRecord.personKey?.personUUID)
    }

    @Test
    fun `should find one high confidence match`() {
      val searchingRecord = createPersonWithNewKey(createRandomLibraPersonDetails())
      val foundRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)
      assertThat(highConfidenceMatch.first().personUUID).isEqualTo(foundRecord.personKey?.personUUID)
    }

    @Test
    fun `should find multiple high confidence match`() {
      val searchingRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      val foundRecords = listOf(
        createPersonWithNewKey(createRandomLibraPersonDetails()),
        createPersonWithNewKey(createRandomLibraPersonDetails()),
        createPersonWithNewKey(createRandomLibraPersonDetails()),
      )

      stubXPersonMatches(
        matchId = searchingRecord.matchId,
        aboveJoin = foundRecords.map { it.matchId },
      )

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)
      assertThat(highConfidenceMatch.first().personUUID).isEqualTo(foundRecords[0].personKey?.personUUID)
    }

    @Test
    fun `should not return low confidence match`() {
      val searchingRecord = createPersonWithNewKey(createRandomLibraPersonDetails())
      val lowConfidenceRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      stubOnePersonMatchBelowFractureThreshold(
        matchId = searchingRecord.matchId,
        matchedRecord = lowConfidenceRecord.matchId,
      )

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return high confidence match that has no UUID`() {
      val searchingRecord = createPerson(createRandomLibraPersonDetails())
      val foundRecord = createPerson(createRandomLibraPersonDetails())

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return high confidence match has been merged to another record`() {
      val searchingRecord = createPerson(createRandomLibraPersonDetails())
      createPersonKey()
        .addPerson(searchingRecord)

      val foundRecord = createPerson(createRandomLibraPersonDetails())
      val mergedToRecord = createPerson(createRandomLibraPersonDetails())
      createPersonKey()
        .addPerson(foundRecord)
        .addPerson(mergedToRecord)

      mergeRecord(sourcePersonEntity = foundRecord, targetPersonEntity = mergedToRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return its self if person match sends it back`() {
      val record = createPersonWithNewKey(createRandomLibraPersonDetails())

      stubOnePersonMatchAboveJoinThreshold(matchId = record.matchId, matchedRecord = record.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(record)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should return highest scoring record`() {
      val searchingRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      val highScoringRecordOne = createPersonWithNewKey(createRandomLibraPersonDetails())
      val highScoringRecordTwo = createPersonWithNewKey(createRandomLibraPersonDetails())
      val highScoringRecordThree = createPersonWithNewKey(createRandomLibraPersonDetails())

      stubPersonMatchScores(
        matchId = searchingRecord.matchId,
        personMatchResponse = listOf(
          PersonMatchScore(
            candidateMatchId = highScoringRecordOne.matchId.toString(),
            candidateMatchWeight = JOIN_THRESHOLD + 1,
            candidateMatchProbability = 0.9999F,
            candidateShouldJoin = true,
            candidateShouldFracture = false,
          ),
          PersonMatchScore(
            candidateMatchId = highScoringRecordTwo.matchId.toString(),
            candidateMatchWeight = JOIN_THRESHOLD + 3,
            candidateMatchProbability = 0.9999F,
            candidateShouldJoin = true,
            candidateShouldFracture = false,
          ),
          PersonMatchScore(
            candidateMatchId = highScoringRecordThree.matchId.toString(),
            candidateMatchWeight = JOIN_THRESHOLD + 2,
            candidateMatchProbability = 0.9999F,
            candidateShouldJoin = true,
            candidateShouldFracture = false,
          ),
        ),
      )

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      assertThat(highConfidenceMatch[0].personUUID).isEqualTo(highScoringRecordTwo.personKey?.personUUID)
      assertThat(highConfidenceMatch[1].personUUID).isEqualTo(highScoringRecordThree.personKey?.personUUID)
      assertThat(highConfidenceMatch[2].personUUID).isEqualTo(highScoringRecordOne.personKey?.personUUID)
    }
  }

  @Nested
  inner class RaceCondition {

    @Test
    fun `should not return high confidence match with recluster merge status`() {
      val searchingRecord = createPerson(createRandomLibraPersonDetails())
      createPersonKey()
        .addPerson(searchingRecord)

      val foundRecord = createPerson(createRandomLibraPersonDetails())
      createPersonKey(UUIDStatusType.RECLUSTER_MERGE)
        .addPerson(foundRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return high confidence match with merged status`() {
      val searchingRecord = createPerson(createRandomLibraPersonDetails())
      createPersonKey()
        .addPerson(searchingRecord)

      val foundRecord = createPerson(createRandomLibraPersonDetails())
      createPersonKey(UUIDStatusType.MERGED)
        .addPerson(foundRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }
  }

  @Nested
  inner class Telemetry {

    @Test
    fun `should log candidate search summary with correct number of clusters`() {
      val searchingRecord = createPersonWithNewKey(createRandomLibraPersonDetails())

      val cluster1 = createPersonKey()
        .addPerson(createRandomLibraPersonDetails())
        .addPerson(createRandomLibraPersonDetails())

      val cluster2 = createPersonKey()
        .addPerson(createRandomLibraPersonDetails())
        .addPerson(createRandomLibraPersonDetails())

      val cluster3 = createPersonKey()
        .addPerson(createRandomLibraPersonDetails())
        .addPerson(createRandomLibraPersonDetails())

      val aboveJoinThresholdResults = cluster1.personEntities.map {
        PersonMatchScore(
          candidateMatchId = it.matchId.toString(),
          candidateMatchWeight = JOIN_THRESHOLD + 1F,
          candidateMatchProbability = 0.9999F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        )
      }
      val belowJoinThresholdResults = cluster2.personEntities.map {
        PersonMatchScore(
          candidateMatchId = it.matchId.toString(),
          candidateMatchWeight = JOIN_THRESHOLD - 1F,
          candidateMatchProbability = 0.9999F,
          candidateShouldJoin = false,
          candidateShouldFracture = false,
        )
      }
      val belowFractureThresholdResults = cluster3.personEntities.map {
        PersonMatchScore(
          candidateMatchId = it.matchId.toString(),
          candidateMatchWeight = FRACTURE_THRESHOLD - 1F,
          candidateMatchProbability = 0.5677F,
          candidateShouldJoin = false,
          candidateShouldFracture = true,
        )
      }
      stubPersonMatchScores(
        matchId = searchingRecord.matchId,
        personMatchResponse = aboveJoinThresholdResults + belowJoinThresholdResults + belowFractureThresholdResults,
      )

      val highConfidenceMatch = personMatchService.findClustersToJoin(searchingRecord)

      assertThat(highConfidenceMatch.first().personUUID).isNotNull()

      checkTelemetry(
        CPR_CANDIDATE_RECORD_SEARCH,
        mapOf(
          "MATCH_ID" to searchingRecord.matchId.toString(),
          "SOURCE_SYSTEM" to LIBRA.name,
          "RECORD_COUNT" to "6",
          "UUID_COUNT" to "2",
          "ABOVE_JOIN_THRESHOLD_COUNT" to "2",
          "ABOVE_FRACTURE_THRESHOLD_COUNT" to "2",
          "BELOW_FRACTURE_THRESHOLD_COUNT" to "2",
        ),
      )
    }
  }

  private fun noCandidateFound(results: List<PersonKeyEntity>) {
    assertThat(results).isEmpty()
  }
}
