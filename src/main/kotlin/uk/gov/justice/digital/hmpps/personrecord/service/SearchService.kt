package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonBlockingRulesRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQueries
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQuery
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE

@Service
class SearchService(
  private val telemetryService: TelemetryService,
  private val matchService: MatchService,
  private val personRepository: PersonBlockingRulesRepository,
) {

  fun processCandidateRecords(matches: List<MatchResult>): PersonEntity? {
    matches.takeIf { matches.size > 1 }?.forEach { record ->
      telemetryService.trackEvent(
        CPR_MATCH_PERSON_DUPLICATE,
        mapOf(
          EventKeys.SOURCE_SYSTEM to record.candidateRecord.sourceSystem.name,
          EventKeys.DEFENDANT_ID to record.candidateRecord.defendantId,
          EventKeys.CRN to (record.candidateRecord.crn ?: ""),
          EventKeys.PRISON_NUMBER to record.candidateRecord.prisonNumber,
          EventKeys.PROBABILITY_SCORE to record.probability.toString(),
        ),
      )
    }
    return matches.firstOrNull()?.candidateRecord
  }

  fun findCandidateRecordsBySourceSystem(person: Person): List<MatchResult> = searchForRecords(person, PersonQueries.findCandidatesBySourceSystem(person))

  fun findCandidateRecordsWithUuid(person: Person, personRecordId: Long? = null): List<MatchResult> {
    val candidates = searchForRecords(person, PersonQueries.findCandidatesWithUuid(person))
    return filterClustersWithExcludeMarker(candidates, personRecordId)
  }

  private fun filterClustersWithExcludeMarker(candidates: List<MatchResult>, personRecordId: Long?): List<MatchResult> {
    val clusters = candidates.groupBy { it.candidateRecord.personKey?.personId }
    val excludedClusters = clusters.filter { (_, records) ->
      records.any { record ->
        record.candidateRecord.overrideMarkers.any { it.markerType == OverrideMarkerType.EXCLUDE && it.markerValue == personRecordId.toString() }
      }
    }.map { it.key }
    return candidates.filter { candidate -> excludedClusters.contains(candidate.candidateRecord.personKey?.personId).not() }
  }

  private fun searchForRecords(person: Person, personQuery: PersonQuery): List<MatchResult> {
    val highConfidenceMatches = mutableListOf<MatchResult>()
    val matchCandidates = personRepository.findMatchCandidates(person, personQuery.query)
    val totalElements = matchCandidates.size

    val batchOfHighConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(matchCandidates, person)
    highConfidenceMatches.addAll(batchOfHighConfidenceMatches)

    telemetryService.trackEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
        EventKeys.RECORD_COUNT to totalElements.toString(),
        EventKeys.SEARCH_VERSION to PersonQueries.SEARCH_VERSION,
        EventKeys.HIGH_CONFIDENCE_COUNT to highConfidenceMatches.count().toString(),
        EventKeys.LOW_CONFIDENCE_COUNT to (totalElements - highConfidenceMatches.count()).toString(),
        EventKeys.QUERY to personQuery.queryName.name,
      ),
    )
    return highConfidenceMatches.toList().sortedByDescending { it.probability }
  }

  companion object {
    const val PAGE_SIZE: Int = 50
  }
}
