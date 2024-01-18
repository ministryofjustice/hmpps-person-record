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
      prisoners?.let {
        if (matchesExistingPrisonerExactly(it, person)) {
          log.debug("Exact Nomis match found - adding prisoner to person record")
          personRecordService.addPrisonerToPerson(personEntity, it[0])
          telemetryService.trackEvent(
            TelemetryEventType.NOMIS_MATCH_FOUND,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncNumber,
              "Prison Number" to it[0].prisonerNumber,
            ),
          )
        } else if (matchesExistingPrisonerPartially(it, person)) {
          log.debug("Partial Nomis match found")
          telemetryService.trackEvent(
            TelemetryEventType.NOMIS_PARTIAL_MATCH_FOUND,
            mapOf(
              "UUID" to personEntity.personId.toString(),
              "PNC" to person.otherIdentifiers?.pncNumber,
              "Prison Number" to it[0].prisonerNumber,
            ),
          )
        }
      }
    }
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
