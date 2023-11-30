package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdValidator

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val pncIdValidator: PNCIdValidator,
  private val defendantRepository: DefendantRepository,
  private val personRecordService: PersonRecordService
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processPersonFromCourtCaseEvent(person: Person) {
    log.debug("Entered processPersonFromCourtCaseEvent")

    // Ensure PNC is present
    if (person.otherIdentifiers?.pncNumber.isNullOrEmpty()) {
      log.info("PNC not present in court case event - no further processing will occur")
      telemetryService.trackEvent(TelemetryEventType.NEW_CASE_MISSING_PNC, emptyMap())
    }

    person.otherIdentifiers?.pncNumber?.let {
      // Validate PNC
      if (!pncIdValidator.isValid(it)) {
        log.info("Invalid PNC encountered in court case event - no further processing will occur")
        telemetryService.trackEvent(TelemetryEventType.NEW_CASE_INVALID_PNC, mapOf("PNC" to it))
      } else {
        val defendants = defendantRepository.findAllByPncNumber(it)

        if (matchesExistingRecordExactly(defendants, person)) {
          log.info("Exactly matching CPR record exists for defendant - no further processing will occur")
          telemetryService.trackEvent(TelemetryEventType.NEW_CASE_EXACT_MATCH, mapOf("PNC" to it))
        } else if (matchesExistingRecordPartially(defendants, person)) {
          log.info("Partially matching CPR record exists for defendant - no further processing will occur")
          telemetryService.trackEvent(TelemetryEventType.NEW_CASE_PARTIAL_MATCH, extractMatchingFields(defendants[0], person))
        } else {
          log.debug("No existing matching records exist - creating new defendant")
          val personRecord = personRecordService.createPersonRecord(person)
          telemetryService.trackEvent(TelemetryEventType.NEW_CASE_PERSON_CREATED, mapOf("UUID" to personRecord.personId.toString(), "PNC" to it))
        }
      }
    }
  }

  private fun extractMatchingFields(defendant: DefendantEntity, person: Person): Map<String, String?> {
    val matchingFields = mutableMapOf<String, String>()
    if (defendant.surname.equals(person.familyName)) {
      matchingFields["Surname"] = defendant.surname.toString()
    }
    if (defendant.forenameOne.equals(person.givenName)) {
      matchingFields["Forename"] = defendant.forenameOne.toString()
    }
    if (defendant.dateOfBirth?.equals(person.dateOfBirth) == true) {
      matchingFields["Date of birth"] = defendant.dateOfBirth.toString()
    }
    return matchingFields
  }

  private fun matchesExistingRecordPartially(defendants: List<DefendantEntity>, person: Person): Boolean {
    // a record in CPR exists with a matching PNC but Surname, forename, or Dob do not match
    return defendants.isNotEmpty()
      .and(defendants.size == 1)
      .and(
        defendants.any {
          it.pncNumber.equals(person.otherIdentifiers?.pncNumber) &&
            (
              it.surname.equals(person.familyName) ||
                it.forenameOne.equals(person.givenName) ||
                it.dateOfBirth?.equals(person.dateOfBirth) == true
              )
        },
      )
  }

  private fun matchesExistingRecordExactly(defendants: List<DefendantEntity>, person: Person): Boolean {
    return defendants.isNotEmpty()
      .and(defendants.size == 1)
      .and(
        defendants.any {
          it.pncNumber.equals(person.otherIdentifiers?.pncNumber) &&
            it.surname.equals(person.familyName) &&
            it.forenameOne.equals(person.givenName) &&
            it.dateOfBirth?.equals(person.dateOfBirth) == true
        },
      )
  }
}
