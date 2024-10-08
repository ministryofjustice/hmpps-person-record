package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonBlockingRulesRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQueries
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQuery
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
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

  fun findCandidateRecordsWithUuid(person: Person): List<MatchResult> = searchForRecords(person, PersonQueries.findCandidatesWithUuid(person))

  private fun searchForRecords(person: Person, personQuery: PersonQuery): List<MatchResult> {
    var pageNumber = 0
    val highConfidenceMatches = mutableListOf<MatchResult>()
    var matchCandidatesPage: Page<PersonEntity>
    do {
      val pageable = PageRequest.of(pageNumber, PAGE_SIZE)

      matchCandidatesPage = personRepository.findMatchCandidates(person, personQuery.query, pageable)
      val matchCandidates = matchCandidatesPage.content
      val uniqueMatchCandidates = matchCandidates.distinctBy { it.id }

      val batchOfHighConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(uniqueMatchCandidates, person)
      highConfidenceMatches.addAll(
        batchOfHighConfidenceMatches.filter { newMatch ->
          highConfidenceMatches.none { it.candidateRecord.id == newMatch.candidateRecord.id }
        },
      )

      pageNumber++
    } while (matchCandidatesPage.hasNext())

    telemetryService.trackEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
        EventKeys.RECORD_COUNT to matchCandidatesPage.totalElements.toString(),
        EventKeys.SEARCH_VERSION to PersonQueries.SEARCH_VERSION,
        EventKeys.HIGH_CONFIDENCE_COUNT to highConfidenceMatches.count().toString(),
        EventKeys.LOW_CONFIDENCE_COUNT to (matchCandidatesPage.totalElements - highConfidenceMatches.count()).toString(),
        EventKeys.QUERY to personQuery.queryName.name,
      ),
    )
    // Return the list of high-confidence matches sorted by descending probability
    return highConfidenceMatches.toList().sortedByDescending { it.probability }
  }

  companion object {
    const val PAGE_SIZE: Int = 50
  }
}
