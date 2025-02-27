package uk.gov.justice.digital.hmpps.personrecord.service.search

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonBlockingRulesRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQueries
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQuery
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE

@Component
class SearchService(
  private val telemetryService: TelemetryService,
  private val matchService: MatchService,
  private val personMatchService: PersonMatchService,
  private val personRepository: PersonBlockingRulesRepository,
) {

  fun searchByAllSourceSystemsAndHasUuid(personEntity: PersonEntity): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = findCandidateRecordsWithUuid(personEntity)
    return processCandidateRecords(highConfidenceMatches)
  }

  private fun processCandidateRecords(matches: List<MatchResult>): PersonEntity? {
    matches.takeIf { matches.size > 1 }?.forEach { record ->
      telemetryService.trackEvent(
        CPR_MATCH_PERSON_DUPLICATE,
        mapOf(
          EventKeys.SOURCE_SYSTEM to record.candidateRecord.sourceSystem.name,
          EventKeys.DEFENDANT_ID to record.candidateRecord.defendantId,
          EventKeys.C_ID to record.candidateRecord.cId,
          EventKeys.CRN to record.candidateRecord.crn,
          EventKeys.PRISON_NUMBER to record.candidateRecord.prisonNumber,
          EventKeys.PROBABILITY_SCORE to record.probability.toString(),
          EventKeys.UUID to record.candidateRecord.personKey?.let { it.personId.toString() },
        ),
      )
    }
    return matches.firstOrNull()?.candidateRecord
  }

  fun findCandidateRecordsWithUuid(personEntity: PersonEntity): List<MatchResult> {
    val searchCriteria = PersonSearchCriteria.from(personEntity)
    personMatchService.getScores(personEntity)
    val candidates = searchForRecords(searchCriteria, PersonQueries.findCandidatesWithUuid(searchCriteria))
    return filterClustersWithExcludeMarker(candidates, searchCriteria.preparedId?.value)
  }

  private fun filterClustersWithExcludeMarker(candidates: List<MatchResult>, personRecordId: Long?): List<MatchResult> {
    val clusters = candidates.groupBy { it.candidateRecord.personKey?.personId }
    val excludedClusters = clusters.filter { (_, records) ->
      records.any { record ->
        record.candidateRecord.overrideMarkers.any { it.markerType == OverrideMarkerType.EXCLUDE && it.markerValue == personRecordId }
      }
    }.map { it.key }
    return candidates.filter { candidate -> excludedClusters.contains(candidate.candidateRecord.personKey?.personId).not() }
  }

  private fun searchForRecords(searchCriteria: PersonSearchCriteria, personQuery: PersonQuery): List<MatchResult> {
    val highConfidenceMatches = mutableListOf<MatchResult>()
    val totalElements = personRepository.countMatchCandidates(personQuery.query, searchCriteria)

    forPage(searchCriteria, personQuery, totalElements) { page ->
      val batchOfHighConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(page.content, searchCriteria)
      highConfidenceMatches.addAll(batchOfHighConfidenceMatches)
    }

    telemetryService.trackEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        EventKeys.SOURCE_SYSTEM to searchCriteria.sourceSystemType.name,
        EventKeys.RECORD_COUNT to totalElements.toString(),
        EventKeys.SEARCH_VERSION to PersonQueries.SEARCH_VERSION,
        EventKeys.UUID_COUNT to highConfidenceMatches.groupBy { match -> match.candidateRecord.personKey?.let { it.personId.toString() } }.size.toString(),
        EventKeys.HIGH_CONFIDENCE_COUNT to highConfidenceMatches.count().toString(),
        EventKeys.LOW_CONFIDENCE_COUNT to (totalElements - highConfidenceMatches.count()).toString(),
        EventKeys.QUERY to personQuery.queryName.name,
      ),
    )
    return highConfidenceMatches.toList().sortedByDescending { it.probability }
  }

  private inline fun forPage(searchCriteria: PersonSearchCriteria, personQuery: PersonQuery, totalElements: Long, page: (Page<PersonEntity>) -> Unit) {
    var pageNumber = 0
    var matchCandidatesPage: Page<PersonEntity>
    do {
      val pageable = PageRequest.of(pageNumber, PAGE_SIZE)

      matchCandidatesPage = personRepository.findMatchCandidates(searchCriteria, personQuery.query, pageable, totalElements)
      page(matchCandidatesPage)

      pageNumber++
    } while (matchCandidatesPage.hasNext())
  }

  companion object {
    const val PAGE_SIZE: Int = 100
  }
}
