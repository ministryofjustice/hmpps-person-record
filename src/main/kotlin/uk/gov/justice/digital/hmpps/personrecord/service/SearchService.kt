package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.MessageType.LIBRA_COURT_CASE
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
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_SEARCH

@Service
class SearchService(
  private val telemetryService: TelemetryService,
  private val matchService: MatchService,
  private val readWriteLockService: ReadWriteLockService,
  private val personRepository: PersonRepository,
) {

  fun findCandidateRecords(person: Person): Page<PersonEntity> {
    val candidates = executeCandidateSearch(person)
    telemetryService.trackEvent(
      CPR_CANDIDATE_RECORD_SEARCH,
      mapOf(
        EventKeys.SOURCE_SYSTEM to SourceSystemType.LIBRA.name,
        EventKeys.RECORD_COUNT to candidates.totalElements.toString(),
        EventKeys.EVENT_TYPE to LIBRA_COURT_CASE.name,
        EventKeys.SEARCH_VERSION to PersonSpecification.SEARCH_VERSION,
      ),
    )
    return candidates
  }

  private fun executeCandidateSearch(person: Person): Page<PersonEntity> {
    val postcodeSpecifications = person.addresses.map { PersonSpecification.levenshteinPostcode(it.postcode) }

    val soundexFirstLastName = Specification.where(
      PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
        .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
    )

    val levenshteinDob = Specification.where(PersonSpecification.levenshteinDate(person.dateOfBirth, PersonSpecification.DOB))
    val levenshteinPostcode = Specification.where(PersonSpecification.combineSpecificationsWithOr(postcodeSpecifications))

    return readWriteLockService.withReadLock {
      personRepository.findAll(
        Specification.where(
          exactMatch(person.sourceSystemType.name, SOURCE_SYSTEM).and(
            exactMatch(person.otherIdentifiers?.pncIdentifier?.toString(), PNC)
              .or(exactMatch(person.driverLicenseNumber, DRIVER_LICENSE_NUMBER))
              .or(exactMatch(person.nationalInsuranceNumber, NI))
              .or(exactMatch(person.otherIdentifiers?.croIdentifier?.toString(), CRO))
              .or(soundexFirstLastName.and(levenshteinDob.or(levenshteinPostcode))),
          ),
        ),
        Pageable.ofSize(PAGE_SIZE),
      )
    }
  }

  fun processCandidateRecords(personEntities: List<PersonEntity>, person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(personEntities, person)
    return when {
      highConfidenceMatches.isNotEmpty() -> handleHighConfidenceMatches(highConfidenceMatches)
      else -> null
    }
  }

  private fun handleHighConfidenceMatches(matches: List<MatchResult>): PersonEntity {
    if (matches.size > 1) {
      matches.forEach { candidate ->
        telemetryService.trackEvent(
          TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE,
          mapOf(
            EventKeys.SOURCE_SYSTEM to candidate.candidateRecord.sourceSystem.name,
            EventKeys.DEFENDANT_ID to candidate.candidateRecord.defendantId,
            EventKeys.CRN to (candidate.candidateRecord.crn ?: ""),
            EventKeys.PRISON_NUMBER to candidate.candidateRecord.prisonNumber,
            EventKeys.PROBABILITY_SCORE to candidate.probability,
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
