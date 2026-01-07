package uk.gov.justice.digital.hmpps.personrecord.service.search

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidMissingRecordResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.visualisecluster.VisualiseCluster
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DiscardableNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import java.util.UUID
@Component
class PersonMatchService(
  private val personMatchClient: PersonMatchClient,
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val jsonMapper: JsonMapper,
) {

  fun findClustersToJoin(personEntity: PersonEntity): List<PersonKeyEntity> = findPersonRecordsAboveFractureThresholdByMatchWeightDesc(personEntity)
    .getClustersThatItCanJoin()
    .collectDistinctClusters()
    .removeExcludedClusters(personEntity)

  fun findPersonRecordsAboveFractureThresholdByMatchWeightDesc(personEntity: PersonEntity): List<PersonMatchResult> {
    val personScores = handleCollectingPersonScores(personEntity).removeSelf(personEntity)
    val aboveFractureThresholdPersonRecords = getPersonRecords(personScores.getClustersAboveFractureThreshold())
      .allowMatchesWithUUID()
      .removeMergedRecords()
      .removeMatchesWhereClusterInInvalidState()
      .logCandidateSearchSummary(personEntity, totalNumberOfScores = personScores.size)
      .sortedByDescending { it.matchWeight }
    return aboveFractureThresholdPersonRecords
  }

  fun examineIsClusterValid(cluster: PersonKeyEntity): IsClusterValidResponse = runBlocking {
    cluster.getRecordsMatchIds().checkClusterIsValid()
  }

  fun examineIsClusterMergeValid(currentCluster: PersonKeyEntity, matchedClusters: List<PersonKeyEntity>): IsClusterValidResponse = runBlocking {
    val records = currentCluster.getRecordsMatchIds() + matchedClusters.map { it.getRecordsMatchIds() }.flatten()
    records.checkClusterIsValid()
  }

  fun retrieveClusterVisualisationSpec(cluster: PersonKeyEntity): VisualiseCluster = personMatchClient.postVisualiseCluster(cluster.getRecordsMatchIds())

  fun saveToPersonMatch(personEntity: PersonEntity): ResponseEntity<Void>? = personMatchClient.postPerson(PersonMatchRecord.from(personEntity))

  fun deleteFromPersonMatch(personEntity: PersonEntity) = try {
    personMatchClient.deletePerson(PersonMatchIdentifier.from(personEntity))
  } catch (_: DiscardableNotFoundException) {
    log.info("ignoring 404 from person match because record has already been deleted")
  }

  private fun IsClusterValidMissingRecordResponse.upsertMissingRecords() = this.unknownIds.forEach { matchId ->
    personRepository.findByMatchId(UUID.fromString(matchId))?.let { saveToPersonMatch(it) }
  }

  private fun handleDecodeOfNotFoundException(exception: NotFound): IsClusterValidMissingRecordResponse {
    val responseBody = exception.responseBodyAsString
    return jsonMapper.readValue<IsClusterValidMissingRecordResponse>(responseBody)
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

  private fun List<PersonKeyEntity>.removeExcludedClusters(personEntity: PersonEntity): List<PersonKeyEntity> {
    val evaluatingClusterScopes: Set<UUID> = personEntity.personKey?.getScopes()?.toSet() ?: personEntity.getScopes()
    return this.filter { evaluatingClusterScopes.intersect(it.getScopes().toSet()).isEmpty() }
  }

  private suspend fun List<String>.checkClusterIsValid(): IsClusterValidResponse = runCatching { personMatchClient.isClusterValid(this) }.fold(
    onSuccess = { it },
    onFailure = { exception ->
      when {
        exception is NotFound -> {
          handleDecodeOfNotFoundException(exception).upsertMissingRecords()
          return personMatchClient.isClusterValid(this)
        }
        else -> throw exception
      }
    },
  )

  private fun PersonKeyEntity.getRecordsMatchIds(): List<String> = this.personEntities.map { it.matchId.toString() }

  private fun List<PersonMatchResult>.collectDistinctClusters(): List<PersonKeyEntity> = this
    .map { it.personEntity }
    .groupBy { it.personKey!! }
    .map { it.key }
    .distinctBy { it.id }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

class PersonMatchResult(
  val shouldJoin: Boolean,
  val matchWeight: Float,
  val personEntity: PersonEntity,
)
