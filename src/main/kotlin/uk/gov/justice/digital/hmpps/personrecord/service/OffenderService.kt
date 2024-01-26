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
  }

  fun processAssociatedOffenders(personEntity: PersonEntity, person: Person) {
    log.debug("Entered processAssociatedOffenders")
    if (featureFlag.isDeliusSearchEnabled()) {
      val offenderDetails = client.getOffenderDetail(SearchDto.from(person))
      offenderDetails?.let {
        if (matchesExistingOffenderExactly(it, person)) {
          log.debug("Exact delius match found - adding offender to person record")
          personRecordService.addOffenderToPerson(personEntity, Person.from(it[0]))
          telemetryService.trackEvent(
            TelemetryEventType.DELIUS_MATCH_FOUND,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
              "CRN" to person.otherIdentifiers?.crn,
            ),
          )
        } else if (matchesExistingOffenderPartially(it, person)) {
          log.debug("Partial Delius match found")
          telemetryService.trackEvent(
            TelemetryEventType.DELIUS_PARTIAL_MATCH_FOUND,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
              "CRN" to person.otherIdentifiers?.crn,
            ),
          )
        }
      }
      if (offenderDetails.isNullOrEmpty()) {
        log.debug("No Delius matching records exist")
        telemetryService.trackEvent(
          TelemetryEventType.DELIUS_NO_MATCH_FOUND,
          mapOf("UUID" to personEntity.personId.toString(), "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId),
        )
      }
    }
  }

  private fun matchesExistingOffenderPartially(offenderDetails: List<OffenderDetail>, person: Person): Boolean {
    return offenderDetails.isNotEmpty()
      .and(offenderDetails.size == 1)
      .and(
        offenderDetails.any {
          person.otherIdentifiers?.pncIdentifier?.isEquivalentTo(PNCIdentifier(it.otherIds.pncNumber)) == true &&
            (
              it.firstName.equals(person.givenName, true) ||
                it.surname.equals(person.familyName, true) ||
                it.dateOfBirth == person.dateOfBirth
              )
        },
      )
  }

  private fun matchesExistingOffenderExactly(offenderDetails: List<OffenderDetail>, person: Person): Boolean {
    return offenderDetails.isNotEmpty()
      .and(offenderDetails.size == 1)
      .and(
        offenderDetails.any {
          person.otherIdentifiers?.pncIdentifier?.isEquivalentTo(PNCIdentifier(it.otherIds.pncNumber)) == true &&
            it.firstName.equals(person.givenName, true) &&
            it.surname.equals(person.familyName, true) &&
            it.dateOfBirth == person.dateOfBirth
        },
      )
  }
}
