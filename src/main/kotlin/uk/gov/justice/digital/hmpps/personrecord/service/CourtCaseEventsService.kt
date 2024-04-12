package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.InvalidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.ValidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.DefendantMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val defendantRepository: DefendantRepository,
  private val personRecordService: PersonRecordService,
  private val offenderService: OffenderService,
  private val prisonerService: PrisonerService,
) {

  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun processPersonFromCourtCaseEvent(person: Person) {
    when (val pncIdentifier = person.otherIdentifiers?.pncIdentifier) {
      is ValidPNCIdentifier -> processValidMessage(pncIdentifier, person)
      is InvalidPNCIdentifier -> trackEvent(TelemetryEventType.INVALID_PNC, mapOf("PNC" to pncIdentifier.invalidValue()))
      else -> trackEvent(TelemetryEventType.MISSING_PNC, emptyMap())
    }
    if (person.otherIdentifiers?.croIdentifier != null) {
      if (!person.otherIdentifiers.croIdentifier.valid) {
        trackEvent(TelemetryEventType.INVALID_CRO, mapOf("CRO" to person.otherIdentifiers.croIdentifier.invalidCro))
      }
    }
  }

  private fun processValidMessage(pncIdentifier: ValidPNCIdentifier, person: Person) {
    trackEvent(TelemetryEventType.VALID_PNC, mapOf("PNC" to pncIdentifier.toString()))
    val defendants = defendantRepository.findAllByPncNumber(pncIdentifier)
    val defendantMatcher = DefendantMatcher(defendants, person)
    when {
      defendantMatcher.isExactMatch() -> exactMatchFound(defendantMatcher, person)
      defendantMatcher.isPartialMatch() -> partialMatchFound(defendantMatcher)
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
      TelemetryEventType.HMCTS_RECORD_CREATED,
      mapOf("UUID" to personRecord.personId.toString(), "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId),
    )
  }

  private fun exactMatchFound(defendantMatcher: DefendantMatcher, person: Person) {
    val elementMap = mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId, "CRN" to defendantMatcher.getMatchingItem().crn, "UUID" to person.personId.toString())
    trackEvent(TelemetryEventType.HMCTS_EXACT_MATCH, elementMap)
  }

  private fun partialMatchFound(defendantMatcher: DefendantMatcher) {
    trackEvent(TelemetryEventType.HMCTS_PARTIAL_MATCH, defendantMatcher.extractMatchingFields(defendantMatcher.getMatchingItem()))
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
