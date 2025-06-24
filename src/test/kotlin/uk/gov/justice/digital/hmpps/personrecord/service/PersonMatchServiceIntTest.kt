package uk.gov.justice.digital.hmpps.personrecord.service

import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class PersonMatchServiceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personMatchService: PersonMatchService

  @Nested
  inner class IsClusterValid {

    @Test
    fun `isClusterValid request sent as a list of matchId`() {
      val personA = createPerson(createExamplePerson())
      val personB = createPerson(createExamplePerson())
      val cluster = createPersonKey()
        .addPerson(personA)
        .addPerson(personB)

      stubClusterIsValid()

      personMatchService.examineIsClusterValid(cluster)

      wiremock.verify(
        postRequestedFor(urlEqualTo("/is-cluster-valid"))
          .withRequestBody(equalToJson("""["${personA.matchId}", "${personB.matchId}"]""")),
      )
    }

    @Test
    fun `should process isClusterValid response`() {
      val personA = createPerson(createExamplePerson())
      val personB = createPerson(createExamplePerson())
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
      val personA = createPerson(createExamplePerson())
      val personB = createPerson(createExamplePerson())
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
      stubClusterIsValid(currentScenarioState = "CLUSTER IS VALID")

      val result = personMatchService.examineIsClusterValid(cluster)
      assertThat(result.isClusterValid).isTrue()
    }
  }

  @Nested
  inner class Scoring {

    @Test
    fun `should find one high confidence match for record not assigned to cluster with override markers`() {
      val searchingRecord = createPerson(createExamplePerson())

      val foundRecord = createPerson(createExamplePerson())
      val overridesRecord = createPerson(createExamplePerson())
      createPersonKey()
        .addPerson(foundRecord)
        .addPerson(overridesRecord)

      excludeRecord(overridesRecord, excludingRecord = searchingRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val person = personRepository.findByMatchId(searchingRecord.matchId)!!
      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(person)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not find one high confidence match if record in searching cluster has override marker with match`() {
      val searchingRecord = createPerson(createExamplePerson())
      val overridesRecord = createPerson(createExamplePerson())
      createPersonKey()
        .addPerson(searchingRecord)
        .addPerson(overridesRecord)

      val foundRecord = createPersonWithNewKey(createExamplePerson())

      excludeRecord(overridesRecord, excludingRecord = foundRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val person = personRepository.findByMatchId(searchingRecord.matchId)!!
      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(person)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should find one high confidence match for record not assigned to cluster`() {
      val searchingRecord = createPerson(createExamplePerson())
      val foundRecord = createPersonWithNewKey(createExamplePerson())

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)
      assertThat(highConfidenceMatch?.matchId).isEqualTo(foundRecord.matchId)
    }

    @Test
    fun `should find one high confidence match`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())
      val foundRecord = createPersonWithNewKey(createExamplePerson())

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)
      assertThat(highConfidenceMatch?.matchId).isEqualTo(foundRecord.matchId)
    }

    @Test
    fun `should find multiple high confidence match`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())

      val foundRecords = listOf(
        createPersonWithNewKey(createExamplePerson()),
        createPersonWithNewKey(createExamplePerson()),
        createPersonWithNewKey(createExamplePerson()),
      )

      stubXPersonMatchHighThresholdMatches(
        matchId = searchingRecord.matchId,
        aboveJoin = foundRecords.map { it.matchId },
      )

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)
      assertThat(highConfidenceMatch?.matchId).isEqualTo(foundRecords[0].matchId)
    }

    @Test
    fun `should not return low confidence match`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())
      val lowConfidenceRecord = createPersonWithNewKey(createExamplePerson())

      stubOnePersonMatchBelowFractureThreshold(
        matchId = searchingRecord.matchId,
        matchedRecord = lowConfidenceRecord.matchId,
      )

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return high confidence match that has no UUID`() {
      val searchingRecord = createPerson(createExamplePerson())
      val foundRecord = createPerson(createExamplePerson())

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return high confidence match has been merged to another record`() {
      val searchingRecord = createPerson(createExamplePerson())
      createPersonKey()
        .addPerson(searchingRecord)

      val foundRecord = createPerson(createExamplePerson())
      val mergedToRecord = createPerson(createExamplePerson())
      createPersonKey()
        .addPerson(foundRecord)
        .addPerson(mergedToRecord)

      mergeRecord(sourcePersonEntity = foundRecord, targetPersonEntity = mergedToRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return its self if person match sends it back`() {
      val record = createPersonWithNewKey(createExamplePerson())

      stubOnePersonMatchAboveJoinThreshold(matchId = record.matchId, matchedRecord = record.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(record)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not find candidate records when exclude marker set`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())
      val excludedRecord = createPersonWithNewKey(createExamplePerson())

      excludeRecord(searchingRecord, excludingRecord = excludedRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = excludedRecord.matchId)

      val person = personRepository.findByMatchId(searchingRecord.matchId)!!
      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(person)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not find candidate records when exclude marker set on different record in matched cluster`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())

      val matchingCluster = createPersonKey()
      val excludedRecord = createPerson(createExamplePerson(), personKeyEntity = matchingCluster)
      val matchRecordOnSameCluster = createPerson(createExamplePerson(), personKeyEntity = matchingCluster)

      excludeRecord(searchingRecord, excludingRecord = excludedRecord)

      stubXPersonMatchHighThresholdMatches(
        matchId = searchingRecord.matchId,
        aboveJoin = listOf(
          excludedRecord.matchId,
          matchRecordOnSameCluster.matchId,
        ),
      )

      val person = personRepository.findByMatchId(searchingRecord.matchId)!!
      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(person)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should return highest scoring record`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())

      val highScoringRecordOne = createPersonWithNewKey(createExamplePerson())
      val highScoringRecordTwo = createPersonWithNewKey(createExamplePerson())

      stubPersonMatchScores(
        matchId = searchingRecord.matchId,
        personMatchResponse = listOf(
          PersonMatchScore(
            candidateMatchId = highScoringRecordOne.matchId.toString(),
            candidateMatchWeight = JOIN_THRESHOLD - 1,
            candidateMatchProbability = 0.9999F,
            candidateShouldJoin = true,
            candidateShouldFracture = false,
          ),
          PersonMatchScore(
            candidateMatchId = highScoringRecordTwo.matchId.toString(),
            candidateMatchWeight = JOIN_THRESHOLD + 1,
            candidateMatchProbability = 0.9999F,
            candidateShouldJoin = true,
            candidateShouldFracture = false,
          ),

        ),
      )

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      assertThat(highConfidenceMatch?.matchId).isEqualTo(highScoringRecordTwo.matchId)
    }
  }

  @Nested
  inner class RaceCondition {

    @Test
    fun `should not return high confidence match with recluster merge status`() {
      val searchingRecord = createPerson(createExamplePerson())
      createPersonKey()
        .addPerson(searchingRecord)

      val foundRecord = createPerson(createExamplePerson())
      createPersonKey(UUIDStatusType.RECLUSTER_MERGE)
        .addPerson(foundRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }

    @Test
    fun `should not return high confidence match with merged status`() {
      val searchingRecord = createPerson(createExamplePerson())
      createPersonKey()
        .addPerson(searchingRecord)

      val foundRecord = createPerson(createExamplePerson())
      createPersonKey(UUIDStatusType.MERGED)
        .addPerson(foundRecord)

      stubOnePersonMatchAboveJoinThreshold(matchId = searchingRecord.matchId, matchedRecord = foundRecord.matchId)

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      noCandidateFound(highConfidenceMatch)
    }
  }

  @Nested
  inner class Telemetry {

    @Test
    fun `should log candidate search summary with correct number of clusters`() {
      val searchingRecord = createPersonWithNewKey(createExamplePerson())

      val cluster1 = createPersonKey()
      val cluster1Records = listOf(
        createPerson(createExamplePerson(), personKeyEntity = cluster1),
        createPerson(createExamplePerson(), personKeyEntity = cluster1),
      )

      val cluster2 = createPersonKey()
      val cluster2Records = listOf(
        createPerson(createExamplePerson(), personKeyEntity = cluster2),
        createPerson(createExamplePerson(), personKeyEntity = cluster2),
      )

      val cluster3 = createPersonKey()
      val cluster3Records = listOf(
        createPerson(createExamplePerson(), personKeyEntity = cluster3),
        createPerson(createExamplePerson(), personKeyEntity = cluster3),
      )

      val aboveJoinThresholdResults = cluster1Records.map {
        PersonMatchScore(
          candidateMatchId = it.matchId.toString(),
          candidateMatchWeight = JOIN_THRESHOLD + 1F,
          candidateMatchProbability = 0.9999F,
          candidateShouldJoin = true,
          candidateShouldFracture = false,
        )
      }
      val belowJoinThresholdResults = cluster2Records.map {
        PersonMatchScore(
          candidateMatchId = it.matchId.toString(),
          candidateMatchWeight = JOIN_THRESHOLD - 1F,
          candidateMatchProbability = 0.9999F,
          candidateShouldJoin = false,
          candidateShouldFracture = false,
        )
      }
      val belowFractureThresholdResults = cluster3Records.map {
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

      val highConfidenceMatch = personMatchService.findHighestMatchThatPersonRecordCanJoin(searchingRecord)

      assertThat(highConfidenceMatch?.matchId).isNotNull()

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

  private fun noCandidateFound(highConfidenceMatch: PersonEntity?) {
    assertThat(highConfidenceMatch).isNull()
  }

  private fun createExamplePerson() = Person(
    firstName = randomName(),
    lastName = randomName(),
    dateOfBirth = randomDate(),
    cId = randomCId(),
    sourceSystem = LIBRA,
  )
}
