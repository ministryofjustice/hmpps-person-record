package uk.gov.justice.digital.hmpps.personrecord.service.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidMissingRecordResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import java.nio.charset.StandardCharsets
import java.util.UUID

@Component
class PersonMatchService(
  private val personMatchClient: PersonMatchClient,
  private val retryExecutor: RetryExecutor,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val objectMapper: ObjectMapper,
) {

  fun findHighestConfidencePersonRecord(personEntity: PersonEntity) = findHighestConfidencePersonRecordsByProbabilityDesc(personEntity).firstOrNull()?.personEntity

  fun findHighestConfidencePersonRecordsByProbabilityDesc(personEntity: PersonEntity): List<PersonMatchResult> = runBlocking {
    val personScores = handleCollectingPersonScores(personEntity).removeSelf(personEntity)
    val highConfidenceRecords = personScores.removeLowQualityMatches()
    val highConfidencePersonRecords = getPersonRecords(highConfidenceRecords)
      .allowMatchesWithUUID()
      .removeMergedRecords()
      .removeMatchesWhereClusterInInvalidState()
      .removeMatchesWhereClusterHasExcludeMarker(personEntity)
      .logCandidateSearchSummary(personEntity, totalNumberOfScores = personScores.size)
      .sortedByDescending { it.probability }
    return@runBlocking highConfidencePersonRecords
  }

  fun examineIsClusterValid(cluster: PersonKeyEntity): IsClusterValidResponse = runBlocking {
    checkClusterIsValid(cluster).fold(
      onSuccess = { it },
      onFailure = { exception ->
        when {
          exception is FeignException.NotFound -> handleNotFoundRecordsIsClusterValid(cluster, exception)
          else -> throw exception
        }
      },
    )
  }

  private suspend fun handleNotFoundRecordsIsClusterValid(cluster: PersonKeyEntity, exception: FeignException.NotFound): IsClusterValidResponse {
    val missingRecords = handleDecodeOfNotFoundException(exception)
    missingRecords.unknownIds.forEach { matchId ->
      personRepository.findByMatchId(UUID.fromString(matchId))?.let { personMatchClient.postPerson(PersonMatchRecord.from(it)) }
    }
    return checkClusterIsValid(cluster).getOrThrow()
  }

  private fun handleDecodeOfNotFoundException(exception: FeignException.NotFound): IsClusterValidMissingRecordResponse {
    val responseBody = exception.responseBody().orElseThrow { throw exception }
    val decodedBody = StandardCharsets.UTF_8.decode(responseBody).toString()
    return objectMapper.readValue<IsClusterValidMissingRecordResponse>(decodedBody)
  }

  private fun getPersonRecords(personScores: List<PersonMatchScore>): List<PersonMatchResult> = personScores.mapNotNull {
    personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))?.let { person ->
      PersonMatchResult(
        probability = it.candidateMatchProbability,
        personEntity = person,
      )
    }
  }

  private fun handleCollectingPersonScores(personEntity: PersonEntity): List<PersonMatchScore> = runBlocking {
    getPersonScores(personEntity).fold(
      onSuccess = { it },
      onFailure = { throw it },
    )
  }

  private fun List<PersonMatchScore>.removeSelf(personEntity: PersonEntity): List<PersonMatchScore> = this.filterNot { score -> score.candidateMatchId == personEntity.matchId.toString() }

  private fun List<PersonMatchScore>.removeLowQualityMatches(): List<PersonMatchScore> = this.filter { candidate -> isAboveThreshold(candidate.candidateMatchProbability) }

  private suspend fun getPersonScores(personEntity: PersonEntity): Result<List<PersonMatchScore>> = kotlin.runCatching {
    retryExecutor.runWithRetryHTTP { personMatchClient.getPersonScores(personEntity.matchId.toString()) }
  }

  private fun isAboveThreshold(score: Float): Boolean = score >= THRESHOLD_SCORE

  private fun List<PersonMatchResult>.allowMatchesWithUUID(): List<PersonMatchResult> = this.filter { it.personEntity.personKey != PersonKeyEntity.empty }

  private fun List<PersonMatchResult>.removeMergedRecords(): List<PersonMatchResult> = this.filter { it.personEntity.mergedTo == null }

  private fun List<PersonMatchResult>.removeMatchesWhereClusterHasExcludeMarker(personEntity: PersonEntity): List<PersonMatchResult> {
    val updatedClusterRecordIds = personEntity.personKey?.getRecordIds() ?: listOf(personEntity.id)
    val excludedClusters = this.collectDistinctClusters().filter { cluster ->
      cluster.collectExcludeOverrideMarkers().any { updatedClusterRecordIds.contains(it.markerValue) }
    }.map { it.id }
    return this.filterNot { match -> excludedClusters.contains(match.personEntity.personKey?.id) }
  }

  private fun List<PersonMatchResult>.removeMatchesWhereClusterInInvalidState(): List<PersonMatchResult> {
    val validStatuses = listOf(UUIDStatusType.ACTIVE, UUIDStatusType.NEEDS_ATTENTION)
    return this.filter { candidate -> validStatuses.contains(candidate.personEntity.personKey?.status) }
  }

  private fun List<PersonMatchResult>.logCandidateSearchSummary(personEntity: PersonEntity, totalNumberOfScores: Int): List<PersonMatchResult> {
    telemetryService.trackPersonEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      personEntity,
      mapOf(
        EventKeys.RECORD_COUNT to totalNumberOfScores.toString(),
        EventKeys.UUID_COUNT to this.groupBy { match -> match.personEntity.personKey?.personUUID?.toString() }.size.toString(),
        EventKeys.HIGH_CONFIDENCE_COUNT to this.count().toString(),
        EventKeys.LOW_CONFIDENCE_COUNT to (totalNumberOfScores - this.count()).toString(),
      ),
    )
    return this
  }

  private suspend fun checkClusterIsValid(cluster: PersonKeyEntity): Result<IsClusterValidResponse> = runCatching {
    personMatchClient.isClusterValid(cluster.getRecordsMatchIds())
  }

  private fun PersonKeyEntity.getRecordsMatchIds(): List<String> = this.personEntities.map { it.matchId.toString() }

  private fun List<PersonMatchResult>.collectDistinctClusters(): List<PersonKeyEntity> = this.map { it.personEntity }.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }

  private companion object {
    const val THRESHOLD_SCORE = 0.999
  }
}

class PersonMatchResult(
  val probability: Float,
  val personEntity: PersonEntity,
)
