package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.CRO
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.NI
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.PNC
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatch
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH

@Service
class SearchService(
  private val telemetryService: TelemetryService,
  private val matchService: MatchService,
  private val readWriteLockService: ReadWriteLockService,
  private val personRepository: PersonRepository,
) {

  fun findCandidateRecords(person: Person, searchBySourceSystem: Boolean = true): List<MatchResult> {
    val highConfidenceMatches = processPagedCandidates(person, searchBySourceSystem)
    return highConfidenceMatches.sortedByDescending { it.probability }
  }

  private fun processPagedCandidates(person: Person, searchBySourceSystem: Boolean): List<MatchResult> {
    val highConfidenceMatches = mutableListOf<MatchResult>()
    val totalElements = forPage(person, searchBySourceSystem) { page ->
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
      ),
    )
    return highConfidenceMatches.toList()
  }

  private inline fun forPage(person: Person, searchBySourceSystem: Boolean, page: (Page<PersonEntity>) -> Unit): Long {
    var pageNum = 0
    var currentPage: Page<PersonEntity>
    do {
      currentPage = executeCandidateSearch(person, pageNum, searchBySourceSystem)
      if (currentPage.hasContent()) {
        page(currentPage)
      }
      pageNum++
    } while (currentPage.hasNext())
    return currentPage.totalElements
  }

  private fun executeCandidateSearch(person: Person, pageNum: Int, searchBySourceSystem: Boolean): Page<PersonEntity> {
    val postcodeSpecifications = person.addresses.map { PersonSpecification.levenshteinPostcode(it.postcode) }

    val soundexFirstLastName = Specification.where(
      PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
        .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
    )

    val exactMatchSourceSystem = exactMatch(person.sourceSystemType.name, SOURCE_SYSTEM, searchBySourceSystem)
    val levenshteinDob = Specification.where(PersonSpecification.levenshteinDate(person.dateOfBirth, PersonSpecification.DOB))
    val levenshteinPostcode = Specification.where(PersonSpecification.combineSpecificationsWithOr(postcodeSpecifications))

    return readWriteLockService.withReadLock {
      personRepository.findAll(
        Specification.where(
          exactMatchSourceSystem.and(
            exactMatch(person.otherIdentifiers?.pncIdentifier?.toString(), PNC)
              .or(exactMatch(person.driverLicenseNumber, DRIVER_LICENSE_NUMBER))
              .or(exactMatch(person.nationalInsuranceNumber, NI))
              .or(exactMatch(person.otherIdentifiers?.croIdentifier?.toString(), CRO))
              .or(soundexFirstLastName.and(levenshteinDob.or(levenshteinPostcode))),
          ),
        ),
        PageRequest.of(pageNum, PAGE_SIZE),
      )
    }
  }

  fun processCandidateRecords(matches: List<MatchResult>): PersonEntity? {
    return when {
      matches.isNotEmpty() -> handleHighConfidenceMatches(matches)
      else -> null
    }
  }

  private fun handleHighConfidenceMatches(matches: List<MatchResult>): PersonEntity {
    if (matches.size > 1) {
      matches.forEach { record ->
        telemetryService.trackEvent(
          TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE,
          mapOf(
            EventKeys.SOURCE_SYSTEM to record.candidateRecord.sourceSystem.name,
            EventKeys.DEFENDANT_ID to record.candidateRecord.defendantId,
            EventKeys.CRN to (record.candidateRecord.crn ?: ""),
            EventKeys.PRISON_NUMBER to record.candidateRecord.prisonNumber,
            EventKeys.PROBABILITY_SCORE to record.probability.toString(),
          ),
        )
      }
    }
    return matches.first().candidateRecord
  }

  companion object {
    const val PAGE_SIZE: Int = 50
  }
}
