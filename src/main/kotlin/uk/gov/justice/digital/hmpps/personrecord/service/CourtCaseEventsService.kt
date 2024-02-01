package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdValidator
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personRecordService: PersonRecordService,
  private val offenderService: OffenderService,
  private val prisonerService: PrisonerService,
) {

  private val pncIdValidator: PNCIdValidator = PNCIdValidator(telemetryService)
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun processPersonFromCourtCaseEvent(person: Person) {
    log.debug("Entered processPersonFromCourtCaseEvent")
    if (person.otherIdentifiers?.pncIdentifier?.pncId.isNullOrEmpty()) {
      telemetryService.trackEvent(TelemetryEventType.MISSING_PNC, emptyMap())
      return
    }
    person.otherIdentifiers?.pncIdentifier?.let { pncIdentifier ->
      val pncId = pncIdentifier.pncId
      if (pncIdValidator.isValid(pncIdentifier)) {
        val defendants = personRepository.findByDefendantsPncNumber(PNCIdentifier(pncId!!))?.defendants.orEmpty()

        if (matchesExistingRecordExactly(defendants, person)) {
          log.info("Exactly matching Person record exists with defendant - no further processing will occur")
          telemetryService.trackEvent(
            TelemetryEventType.NEW_CASE_EXACT_MATCH,
            mapOf("PNC" to pncId, "CRN" to defendants[0].crn, "UUID" to person.personId.toString()),
          )
        } else if (matchesExistingRecordPartially(defendants, person)) {
          log.info("Partially matching Person record exists with defendant - no further processing will occur")
          telemetryService.trackEvent(
            TelemetryEventType.NEW_CASE_PARTIAL_MATCH,
            extractMatchingFields(defendants[0], person),
          )
        } else if (defendants.isEmpty()) {
          log.debug("No existing matching Person record exists - creating new person and defendant with pnc $pncId")
          val personRecord = personRecordService.createNewPersonAndDefendant(person)
          // option 1 - catch the expected error here and log NEW_CASE_EXACT_MATCH
          // option 2 - introduce a unique constraint then catch ConstraintViolationException and log NEW_CASE_EXACT_MATCH
          // option 3 - make this method retryable on the expected error
          // option 4 - slow down message processing
          // option 5 - see if we can make processing synchronous in code - X
          // option 5 - see if we can make processing synchronous in SQS Listener Config

          telemetryService.trackEvent(
            TelemetryEventType.NEW_CASE_PERSON_CREATED,
            mapOf("UUID" to personRecord.personId.toString(), "PNC" to pncId),
          )
          personRecord.let {
            offenderService.processAssociatedOffenders(it, person)
            prisonerService.processAssociatedPrisoners(it, person)
          }
        } // what if defendants is not empty?
      }
    }
  }

  private fun extractMatchingFields(defendant: DefendantEntity, person: Person): Map<String, String?> =
    mapOf(
      "Surname" to if (defendant.surname.equals(person.familyName)) defendant.surname else null,
      "Forename" to if (defendant.forenameOne.equals(person.givenName)) defendant.forenameOne else null,
      "Date of birth" to if (defendant.dateOfBirth?.equals(person.dateOfBirth) == true) defendant.dateOfBirth.toString() else null,
    ).filterValues { it != null }

  private fun matchesExistingRecordPartially(defendants: List<DefendantEntity>, person: Person): Boolean {
    // a record in CPR exists with a matching PNC but Surname, forename, or Dob do not match
    return defendants.isNotEmpty()
      .and(defendants.size == 1)
      .and(
        defendants.any {
          person.otherIdentifiers?.pncIdentifier == it.pncNumber &&
            (
              it.surname.equals(person.familyName, true) ||
                it.forenameOne.equals(person.givenName, true) ||
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
          person.otherIdentifiers?.pncIdentifier == it.pncNumber &&
            it.surname.equals(person.familyName, true) &&
            it.forenameOne.equals(person.givenName, true) &&
            it.dateOfBirth?.equals(person.dateOfBirth) == true
        },
      )
  }
}
