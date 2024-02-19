package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.InvalidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.ValidPNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.DefendantMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MISSING_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_CASE_EXACT_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_CASE_PARTIAL_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.NEW_CASE_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.VALID_PNC

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personRecordService: PersonRecordService,
  private val offenderService: OffenderService,
  private val prisonerService: PrisonerService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun processPersonFromCourtCaseEvent(person: Person) {
    log.debug("Entered processPersonFromCourtCaseEvent")
    when (val pncIdentifier = person.otherIdentifiers?.pncIdentifier) {
      is ValidPNCIdentifier -> processValidMessage(pncIdentifier, person)
      is InvalidPNCIdentifier -> trackEvent(INVALID_PNC, mapOf("PNC" to pncIdentifier.invalidValue()))
      else -> trackEvent(MISSING_PNC, emptyMap())
    }
  }

  private fun processValidMessage(pncIdentifier: ValidPNCIdentifier, person: Person) {
    trackEvent(VALID_PNC, mapOf("PNC" to pncIdentifier.toString()))
    val defendants = personRepository.findByDefendantsPncNumber(pncIdentifier)?.defendants.orEmpty()
    val defendantMatcher = DefendantMatcher(defendants, person)
    val pncId = pncIdentifier.pncId
    when {
      defendantMatcher.isExactMatch() -> exactMatchFound(defendantMatcher, person, pncId)
      defendantMatcher.isPartialMatch() -> partialMatchFound(defendantMatcher)
      else -> {
        createNewPersonRecordAndProcess(person, pncId)
      }
    }
  }

  private fun createNewPersonRecordAndProcess(person: Person, pnc: String) {
    log.debug("No existing matching Person record exists - creating new person and defendant with pnc $pnc")
    val personRecord = personRecordService.createNewPersonAndDefendant(person)
    personRecord.let {
      offenderService.processAssociatedOffenders(it, person)
      prisonerService.processAssociatedPrisoners(it, person)
    }
    trackEvent(
      NEW_CASE_PERSON_CREATED,
      mapOf("UUID" to personRecord.personId.toString(), "PNC" to pnc),
    )
  }
  private fun exactMatchFound(defendantMatcher: DefendantMatcher, person: Person, pnc: String) {
    log.info("Exactly matching Person record exists with defendant - no further processing will occur")
    val elementMap = mapOf("PNC" to pnc, "CRN" to defendantMatcher.getMatchingItem().crn, "UUID" to person.personId.toString())
    trackEvent(NEW_CASE_EXACT_MATCH, elementMap)
  }

  private fun partialMatchFound(defendantMatcher: DefendantMatcher) {
    log.info("Partially matching Person record exists with defendant - no further processing will occur")
    trackEvent(NEW_CASE_PARTIAL_MATCH, defendantMatcher.extractMatchingFields(defendantMatcher.getMatchingItem()))
  }
  private fun trackEvent(
    eventType: TelemetryEventType,
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
