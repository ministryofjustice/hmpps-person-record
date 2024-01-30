package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier

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
      val offenderDetails = client.getOffenderDetail(SearchDto.from(person))

      if (offenderDetails.isNullOrEmpty()) {
        logAndTrackEvent(NO_RECORDS_MESSAGE, TelemetryEventType.DELIUS_NO_MATCH_FOUND, personEntity, person)
      } else {
        when {
          matchesExistingOffenderExactly(offenderDetails, person) -> {
            logAndTrackEvent(EXACT_MATCH_MESSAGE, TelemetryEventType.DELIUS_MATCH_FOUND, personEntity, person)
            personRecordService.addOffenderToPerson(personEntity, Person.from(offenderDetails[0]))
          }

          matchesExistingOffenderPartially(offenderDetails, person) -> {
            logAndTrackEvent(PARTIAL_MATCH_MESSAGE, TelemetryEventType.DELIUS_PARTIAL_MATCH_FOUND, personEntity, person)
          }

          multipleOffendersMatch(offenderDetails, person) -> {
            offenderDetails.forEach { offender ->
              logAndTrackEvent(MULTIPLE_MATCHES_MESSAGE, TelemetryEventType.DELIUS_MATCH_FOUND, personEntity, person)
              personRecordService.addOffenderToPerson(personEntity, Person.from(offender))
            }
          }
        }
      }
    }
  }

  private fun isMatchingOffender(person: Person, offenderDetail: OffenderDetail) = person.otherIdentifiers?.pncIdentifier == PNCIdentifier(offenderDetail.otherIds.pncNumber) &&
    offenderDetail.firstName.equals(person.givenName, true) &&
    offenderDetail.surname.equals(person.familyName, true) &&
    offenderDetail.dateOfBirth == person.dateOfBirth

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
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "CRN" to person.otherIdentifiers?.crn,
      ),
    )
  }

  private fun matchesExistingOffenderPartially(offenderDetails: List<OffenderDetail>, person: Person): Boolean {
    return (offenderDetails.size == 1)
      .and(
        offenderDetails.any {
          person.otherIdentifiers?.pncIdentifier == PNCIdentifier(it.otherIds.pncNumber) &&
            (
              it.firstName.equals(person.givenName, true) ||
                it.surname.equals(person.familyName, true) ||
                it.dateOfBirth == person.dateOfBirth
              )
        },
      )
  }

  private fun matchesExistingOffenderExactly(offenderDetails: List<OffenderDetail>, person: Person): Boolean {
    return (offenderDetails.size == 1)
      .and(offenderDetails.any { isMatchingOffender(person, it) })
  }

  private fun multipleOffendersMatch(offenderDetails: List<OffenderDetail>, person: Person): Boolean {
    return (offenderDetails.size > 1)
      .and(offenderDetails.all { isMatchingOffender(person, it) })
  }
}
