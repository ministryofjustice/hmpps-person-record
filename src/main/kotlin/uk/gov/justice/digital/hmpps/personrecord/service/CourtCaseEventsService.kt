package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.ValidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.DefendantMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_EXACT_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_PARTIAL_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_CRO
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.SPLINK_MATCH_SCORE

private const val MAXMATCHES: Int = 1

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val defendantRepository: DefendantRepository,
  private val personRecordService: PersonRecordService,
  private val offenderService: OffenderService,
  private val prisonerService: PrisonerService,
  private val matchService: MatchService,
) {

  @Transactional(isolation = SERIALIZABLE)
  fun processPersonFromCourtCaseEvent(person: Person) {
    if (person.otherIdentifiers?.croIdentifier?.valid == false) {
      trackEvent(INVALID_CRO, mapOf("CRO" to person.otherIdentifiers.croIdentifier.inputCro))
    }
    processMessage(person)
  }

  private fun processMessage(person: Person) {
    val pncNumber = person.otherIdentifiers?.pncIdentifier!!
    val defendants = when {
      pncNumber is ValidPNCIdentifier -> defendantRepository.findAllByPncNumber(pncNumber)
      else -> findByFirstNameSurname(person.givenName, person.familyName)
    }

    val defendantMatcher = DefendantMatcher(defendants, person)
    when {
      defendantMatcher.isExactMatch() -> exactMatchFound(defendantMatcher, person)
      defendantMatcher.isPartialMatch() -> partialMatchFound(defendantMatcher, person)
      else -> {
        createNewPersonRecordAndProcess(person)
      }
    }
  }

  private fun findByFirstNameSurname(firstName: String? = "firstName", surname: String? = "surname"): List<DefendantEntity> {
    return defendantRepository.findAllByFirstNameAndSurname(firstName!!, surname!!)
  }

  private fun createNewPersonRecordAndProcess(person: Person) {
    val personRecord = personRecordService.createNewPersonAndDefendant(person)
    personRecord.let {
      offenderService.processAssociatedOffenders(it, person)
      prisonerService.processAssociatedPrisoners(it, person)
    }
    trackEvent(
      HMCTS_RECORD_CREATED,
      mapOf("UUID" to personRecord.personId.toString(), "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId),
    )
  }

  private fun exactMatchFound(defendantMatcher: DefendantMatcher, person: Person) {
    val elementMap = mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId, "CRN" to defendantMatcher.getMatchingItem().crn, "UUID" to person.personId.toString())
    trackEvent(HMCTS_EXACT_MATCH, elementMap)
  }

  private fun partialMatchFound(defendantMatcher: DefendantMatcher, person: Person) {
    trackEvent(HMCTS_PARTIAL_MATCH, defendantMatcher.extractMatchingFields(defendantMatcher.getMatchingItem()))

    val matchResults = defendantMatcher.items!!.take(MAXMATCHES).map { matchResult(it, person) }

    matchResults.forEach { matchResult ->
      trackEvent(
        SPLINK_MATCH_SCORE,
        mapOf(
          "Match Probability Score" to matchResult.probability,
          "Candidate Record UUID" to matchResult.candidateRecordUUID,
          "Candidate Record Identifier Type" to matchResult.candidateRecordIdentifierType,
          "Candidate Record Identifier" to matchResult.candidateRecordIdentifier,
          "New Record Identifier Type" to matchResult.newRecordIdentifierType,
          "New Record Identifier" to matchResult.newRecordIdentifier,
        ),
      )
    }
  }

  private fun matchResult(
    candidate: DefendantEntity,
    person: Person,
  ): MatchResult {
    if (nullDate(candidate, person)) {
      return MatchResult("not scored, no date of birth", candidate.person?.personId.toString(), "defendantId", candidate.defendantId ?: "defendant1", "defendantId", person.defendantId ?: "defendant2")
    }
    return matchService.score(candidate, person)
  }

  private fun nullDate(
    candidate: DefendantEntity,
    person: Person,
  ) = candidate.dateOfBirth == null || person.dateOfBirth == null

  private fun trackEvent(
    eventType: TelemetryEventType,
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
