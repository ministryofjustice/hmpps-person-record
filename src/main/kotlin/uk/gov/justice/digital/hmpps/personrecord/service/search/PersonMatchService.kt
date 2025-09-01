package uk.gov.justice.digital.hmpps.personrecord.service.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidMissingRecordResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import java.util.UUID

@Component
class PersonMatchService(
  private val personMatchClient: PersonMatchClient,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val objectMapper: ObjectMapper,
) {

  fun findClustersToJoin(personEntity: PersonEntity) = findPersonRecordsAboveFractureThresholdByMatchWeightDesc(personEntity)
    .getClustersThatItCanJoin()

  fun findPersonRecordsAboveFractureThresholdByMatchWeightDesc(personEntity: PersonEntity): List<PersonMatchResult> = runBlocking {
    val personScores = handleCollectingPersonScores(personEntity).removeSelf(personEntity)
    val aboveFractureThresholdPersonRecords = getPersonRecords(personScores.getClustersAboveFractureThreshold())
      .allowMatchesWithUUID()
      .removeMergedRecords()
      .removeMatchesWhereClusterInInvalidState()
      .removeMatchesWhereClusterHasExcludeMarker(personEntity)
      .logCandidateSearchSummary(personEntity, totalNumberOfScores = personScores.size)
      .sortedByDescending { it.matchWeight }
    return@runBlocking aboveFractureThresholdPersonRecords
  }

  fun examineIsClusterValid(cluster: PersonKeyEntity): IsClusterValidResponse = runBlocking {
    checkClusterIsValid(cluster).fold(
      onSuccess = { it },
      onFailure = { exception ->
        when {
          exception is NotFound -> handleNotFoundRecordsIsClusterValid(exception) { runBlocking { checkClusterIsValid(cluster).getOrThrow() } }
          else -> throw exception
        }
      },
    )
  }

  fun examineIsClusterMergeValid(clusters: List<PersonKeyEntity>): IsClusterValidResponse = runBlocking {
    checkClusterMergeIsValid(clusters).fold(
      onSuccess = { it },
      onFailure = { exception ->
        when {
          exception is NotFound -> handleNotFoundRecordsIsClusterValid(exception) { runBlocking { checkClusterMergeIsValid(clusters).getOrThrow() } }
          else -> throw exception
        }
      },
    )
  }

  fun saveToPersonMatch(personEntity: PersonEntity): ResponseEntity<Void>? = runBlocking { personMatchClient.postPerson(PersonMatchRecord.from(personEntity)) }

  fun deleteFromPersonMatch(personEntity: PersonEntity) = runBlocking { runCatching { personMatchClient.deletePerson(PersonMatchIdentifier.from(personEntity)) } }

  private suspend fun handleNotFoundRecordsIsClusterValid(exception: NotFound, isClusterAction: () -> IsClusterValidResponse): IsClusterValidResponse {
    val missingRecords = handleDecodeOfNotFoundException(exception)
    missingRecords.upsertMissingRecords()
    return isClusterAction()
  }

  private fun IsClusterValidMissingRecordResponse.upsertMissingRecords() = this.unknownIds.forEach { matchId ->
    personRepository.findByMatchId(UUID.fromString(matchId))?.let { saveToPersonMatch(it) }
  }

  private fun handleDecodeOfNotFoundException(exception: NotFound): IsClusterValidMissingRecordResponse {
    val responseBody = exception.responseBodyAsString
    return objectMapper.readValue<IsClusterValidMissingRecordResponse>(responseBody)
  }

  private fun getPersonRecords(personScores: List<PersonMatchScore>): List<PersonMatchResult> = personScores.mapNotNull {
    personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))?.let { person ->
      PersonMatchResult(
        shouldJoin = it.candidateShouldJoin,
        matchWeight = it.candidateMatchWeight,
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

  private fun List<PersonMatchResult>.getClustersThatItCanJoin(): List<PersonMatchResult> = this.filter { it.shouldJoin }

  private fun List<PersonMatchScore>.getClustersAboveFractureThreshold(): List<PersonMatchScore> = this.filter { candidate -> candidate.candidateShouldFracture.not() }

  private suspend fun getPersonScores(personEntity: PersonEntity): Result<List<PersonMatchScore>> = kotlin.runCatching {
    personMatchClient.getPersonScores(personEntity.matchId.toString())
  }

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
    val canJoinCount = this.getClustersThatItCanJoin()
    telemetryService.trackPersonEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      personEntity,
      mapOf(
        EventKeys.RECORD_COUNT to totalNumberOfScores.toString(),
        EventKeys.UUID_COUNT to this.groupBy { match -> match.personEntity.personKey?.personUUID?.toString() }.size.toString(),
        EventKeys.ABOVE_JOIN_THRESHOLD_COUNT to canJoinCount.count().toString(),
        EventKeys.ABOVE_FRACTURE_THRESHOLD_COUNT to (this.count() - canJoinCount.count()).toString(),
        EventKeys.BELOW_FRACTURE_THRESHOLD_COUNT to (totalNumberOfScores - this.count()).toString(),
      ),
    )
    return this
  }

  private suspend fun checkClusterIsValid(cluster: PersonKeyEntity): Result<IsClusterValidResponse> = runCatching {
    personMatchClient.isClusterValid(cluster.getRecordsMatchIds())
  }

  private suspend fun checkClusterMergeIsValid(clusters: List<PersonKeyEntity>): Result<IsClusterValidResponse> = runCatching {
    personMatchClient.isClusterValid(clusters.map { it.getRecordsMatchIds() }.flatten())
  }

  private fun PersonKeyEntity.getRecordsMatchIds(): List<String> = this.personEntities.map { it.matchId.toString() }

  private fun List<PersonMatchResult>.collectDistinctClusters(): List<PersonKeyEntity> = this.map { it.personEntity }.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }
}

class PersonMatchResult(
  val shouldJoin: Boolean,
  val matchWeight: Float,
  val personEntity: PersonEntity,
)
