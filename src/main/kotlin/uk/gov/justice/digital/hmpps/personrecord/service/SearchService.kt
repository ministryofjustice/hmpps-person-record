package uk.gov.justice.digital.hmpps.personrecord.service

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
    val findPerson = personRepository.findMatchCandidates(person)
    val totalElements = findPerson.size

    val batchOfHighConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(findPerson, person)
    highConfidenceMatches.addAll(batchOfHighConfidenceMatches)

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

  companion object {
    const val PAGE_SIZE: Int = 50
  }
}
