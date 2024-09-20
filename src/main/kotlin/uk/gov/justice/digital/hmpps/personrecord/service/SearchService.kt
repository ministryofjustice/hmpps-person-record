package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries.PersonQuery
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries.findCandidatesBySourceSystem
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries.findCandidatesWithUuid
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE

@Service
class SearchService(
  private val telemetryService: TelemetryService,
  private val matchService: MatchService,
  private val personRepository: PersonRepository,
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

  fun findCandidateRecordsBySourceSystem(person: Person): List<MatchResult> = searchForRecords(person, findCandidatesBySourceSystem(person))

  fun findCandidateRecordsWithUuid(person: Person): List<MatchResult> = searchForRecords(person, findCandidatesWithUuid(person))

  private fun searchForRecords(person: Person, personQuery: PersonQuery): List<MatchResult> {
    val highConfidenceMatches = mutableListOf<MatchResult>()
    val totalElements = forPage(personQuery.query) { page ->
      val batchOfHighConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(page.content, person)
      highConfidenceMatches.addAll(batchOfHighConfidenceMatches)
    }
    telemetryService.trackEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
        EventKeys.RECORD_COUNT to totalElements.toString(),
        EventKeys.SEARCH_VERSION to PersonSpecification.SEARCH_VERSION,
        EventKeys.HIGH_CONFIDENCE_COUNT to highConfidenceMatches.count().toString(),
        EventKeys.LOW_CONFIDENCE_COUNT to (totalElements - highConfidenceMatches.count()).toString(),
        EventKeys.QUERY to personQuery.queryName.name,
      ),
    )
    return highConfidenceMatches.toList().sortedByDescending { it.probability }
  }

  private inline fun forPage(query: Specification<PersonEntity>, page: (Page<PersonEntity>) -> Unit): Long {
    var pageNum = 0
    var currentPage: Page<PersonEntity>
    do {
      currentPage = executePagedQuery(query, pageNum)
      if (currentPage.hasContent()) {
        page(currentPage)
      }
      pageNum++
    } while (currentPage.hasNext())
    return currentPage.totalElements
  }

  private fun executePagedQuery(query: Specification<PersonEntity>, pageNum: Int): Page<PersonEntity> =
    personRepository.findAll(query, PageRequest.of(pageNum, PAGE_SIZE))

  companion object {
    const val PAGE_SIZE: Int = 50
  }
}
