package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.OffenderMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class OffenderService(
  private val telemetryService: TelemetryService,
  private val personRecordService: PersonRecordService,
  private val client: ProbationOffenderSearchClient,
  private val featureFlag: FeatureFlag,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val NO_RECORDS_MESSAGE = "No Delius matching records exist"
    const val EXACT_MATCH_MESSAGE = "Exact delius match found - adding offender to person record"
    const val PARTIAL_MATCH_MESSAGE = "Partial Delius match found"
    const val MULTIPLE_MATCHES_MESSAGE = "Multiple Delius matches found"
  }

  fun processAssociatedOffenders(personEntity: PersonEntity, person: Person) {
    log.debug("Entered processAssociatedOffenders")

    if (featureFlag.isDeliusSearchEnabled()) {
      val offenderMatcher = getOffenderMatcher(person)
      when {
        offenderMatcher.isPncDoesNotMatch() -> pncDoesNotMatch(offenderMatcher, personEntity, person)
        offenderMatcher.isExactMatch() -> exactMatchFound(offenderMatcher, personEntity, person)
        offenderMatcher.isPartialMatch() -> partialMatchFound(personEntity, person)
        offenderMatcher.isMultipleMatch() -> multipleMatchesFound(offenderMatcher, personEntity, person)
        else -> noRecordsFound(personEntity, person)
      }
    }
  }

  private fun pncDoesNotMatch(offenderMatcher: OffenderMatcher, personEntity: PersonEntity, person: Person) {
    offenderMatcher.offenderDetails?.let { trackPncMismatchEvent(it, personEntity, person) }
  }

  private fun getOffenderMatcher(person: Person): OffenderMatcher {
    val offenderDetails = client.getOffenderDetail(SearchDto.from(person))
    return OffenderMatcher(offenderDetails, person)
  }

  private fun noRecordsFound(personEntity: PersonEntity, person: Person) {
    logAndTrackEvent(NO_RECORDS_MESSAGE, TelemetryEventType.DELIUS_NO_MATCH_FOUND, personEntity, person)
  }

  private fun partialMatchFound(personEntity: PersonEntity, person: Person) {
    logAndTrackEvent(PARTIAL_MATCH_MESSAGE, TelemetryEventType.DELIUS_PARTIAL_MATCH_FOUND, personEntity, person)
  }

  private fun exactMatchFound(offenderMatches: OffenderMatcher, personEntity: PersonEntity, person: Person) {
    logAndTrackEvent(EXACT_MATCH_MESSAGE, TelemetryEventType.DELIUS_MATCH_FOUND, personEntity, person)
    val offenderDetail = offenderMatches.getOffenderDetail()
    personRecordService.addOffenderToPerson(personEntity, Person.from(offenderDetail))
  }

  private fun multipleMatchesFound(offenderMatches: OffenderMatcher, personEntity: PersonEntity, person: Person) {
    val allMatchingOffenders = offenderMatches.getAllMatchingOffenders()
    allMatchingOffenders?.forEach { offenderDetail ->
      logAndTrackEvent(MULTIPLE_MATCHES_MESSAGE, TelemetryEventType.DELIUS_MATCH_FOUND, personEntity, person)
      personRecordService.addOffenderToPerson(personEntity, Person.from(offenderDetail))
    }
  }

  private fun trackPncMismatchEvent(
    offenderDetails: List<OffenderDetail>,
    personEntity: PersonEntity,
    person: Person,
  ) {
    telemetryService.trackEvent(
      TelemetryEventType.DELIUS_PNC_MISMATCH,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC searched for" to person.otherIdentifiers?.pncIdentifier.toString(),
        "PNC returned from search" to offenderDetails.joinToString(" ") { it.otherIds.pncNumber.toString() },
        "PRISON NUMBER" to offenderDetails.singleOrNull()?.otherIds?.nomsNumber,
      ).filterValues { !it.isNullOrBlank() },
    )
  }
  private fun logAndTrackEvent(
    logMessage: String,
    eventType: TelemetryEventType,
    personEntity: PersonEntity,
    person: Person,
  ) {
    log.debug(logMessage)
    telemetryService.trackEvent(
      eventType,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier.toString(),
        "CRN" to person.otherIdentifiers?.crn,
        "PRISON NUMBER" to person.otherIdentifiers?.prisonNumber,
      ).filterValues { !it.isNullOrBlank() },
    )
  }
}
