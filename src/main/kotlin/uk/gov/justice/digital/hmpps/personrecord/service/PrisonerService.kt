package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PossibleMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PrisonerService(
  private val telemetryService: TelemetryService,
  private val personRecordService: PersonRecordService,
  private val client: PrisonerSearchClient,
  private val featureFlag: FeatureFlag,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processAssociatedPrisoners(personEntity: PersonEntity, person: Person) {
    log.debug("Entered processAssociatedPrisoners")
    if (featureFlag.isNomisSearchEnabled()) {
      val prisoners = client.findPossibleMatches(PossibleMatchCriteria.from(person))
      log.debug("Number of prisoners returned from Nomis for PNC ${person.otherIdentifiers?.pncNumber} = ${prisoners?.size ?: 0}")
      prisoners?.let { prisonerList ->
        if (pncIdentifierDoesNotMatch(prisonerList, person)) {
          log.debug("Nomis PNC Id does not match that of person")
          telemetryService.trackEvent(
            TelemetryEventType.NOMIS_PNC_DOES_NOT_MATCH,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncNumber,
              "Prison Number" to prisonerList[0].prisonerNumber,
            ),
          )
        } else if (matchesExistingPrisonerExactly(prisonerList, person)) {
          log.debug("Exact Nomis match found - adding prisoner to person record")
          personRecordService.addPrisonerToPerson(personEntity, prisonerList[0])
          telemetryService.trackEvent(
            TelemetryEventType.NOMIS_MATCH_FOUND,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncNumber,
              "Prison Number" to prisonerList[0].prisonerNumber,
            ),
          )
        } else if (matchesExistingPrisonerPartially(prisonerList, person)) {
          log.debug("Partial Nomis match found")
          telemetryService.trackEvent(
            TelemetryEventType.NOMIS_PARTIAL_MATCH_FOUND,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncNumber,
              "Prison Number" to prisonerList[0].prisonerNumber,
            ),
          )
        }
      }
      if (prisoners.isNullOrEmpty()) {
        log.debug("No Nomis matching records exist")
        telemetryService.trackEvent(
          TelemetryEventType.NOMIS_NO_MATCH_FOUND,
          mapOf("UUID" to personEntity.personId.toString(), "PNC" to person.otherIdentifiers?.pncNumber),
        )
      }
    }
  }

  private fun pncIdentifierDoesNotMatch(prisonerList: List<Prisoner>, person: Person): Boolean {
    return prisonerList.none { it.pncNumber.equals(person.otherIdentifiers?.pncNumber) }
  }

  private fun matchesExistingPrisonerPartially(prisoners: List<Prisoner>, person: Person): Boolean {
    return prisoners.isNotEmpty()
      .and(prisoners.size == 1)
      .and(
        prisoners.any {
          it.pncNumber.equals(person.otherIdentifiers?.pncNumber) &&
            (
              it.firstName.equals(person.givenName, true) ||
                it.lastName.equals(person.familyName, true) ||
                it.dateOfBirth == person.dateOfBirth
              )
        },
      )
  }

  private fun matchesExistingPrisonerExactly(prisoners: List<Prisoner>, person: Person): Boolean {
    return prisoners.singleOrNull {
      it.pncNumber.equals(person.otherIdentifiers?.pncNumber) &&
        it.firstName.equals(person.givenName, true) &&
        it.lastName.equals(person.familyName, true) &&
        it.dateOfBirth == person.dateOfBirth
    } != null
  }
}
