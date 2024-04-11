package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.InvalidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.ValidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.DefendantMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_EXACT_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_PARTIAL_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MISSING_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.SPLINK_MATCH_SCORE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.VALID_PNC

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val defendantRepository: DefendantRepository,
  private val personRecordService: PersonRecordService,
  private val offenderService: OffenderService,
  private val prisonerService: PrisonerService,
  private val matchingService: MatchingService,
) {

  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun processPersonFromCourtCaseEvent(person: Person) {
    when (val pncIdentifier = person.otherIdentifiers?.pncIdentifier) {
      is ValidPNCIdentifier -> processValidMessage(pncIdentifier, person)
      is InvalidPNCIdentifier -> trackEvent(INVALID_PNC, mapOf("PNC" to pncIdentifier.invalidValue()))
      else -> trackEvent(MISSING_PNC, emptyMap())
    }
  }

  private fun processValidMessage(pncIdentifier: ValidPNCIdentifier, person: Person) {
    trackEvent(VALID_PNC, mapOf("PNC" to pncIdentifier.toString()))
    val defendants = defendantRepository.findAllByPncNumber(pncIdentifier)
    val defendantMatcher = DefendantMatcher(defendants, person)
    when {
      defendantMatcher.isExactMatch() -> exactMatchFound(defendantMatcher, person)
      defendantMatcher.isPartialMatch() -> partialMatchFound(defendantMatcher, person)
      else -> {
        createNewPersonRecordAndProcess(person)
      }
    }
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
    val matchResult = matchingService.score(defendantMatcher.getAllMatchingItems()!!, person)
    trackEvent(
      SPLINK_MATCH_SCORE,
      mapOf(
        "Match Probability Score" to matchResult.probability,
        "Candidate Record UUID" to "UUID",
        "Candidate Record Identifier Type" to "defendantID",
        "Candidate Record Identifier" to "123456",
        "New Record Identifier Type" to "defendantID",
        "New Record Identifier" to matchResult.newRecordIdentifier,
      ),
    )
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
