package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.DefendantMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_EXACT_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_CRO

@Service
class CourtCaseEventsService(
  private val telemetryService: TelemetryService,
  private val defendantRepository: DefendantRepository,
) {

  @Transactional(isolation = SERIALIZABLE)
  fun processPersonFromCourtCaseEvent(person: Person) {
    if (person.otherIdentifiers?.croIdentifier?.valid == false) {
      trackEvent(INVALID_CRO, mapOf("CRO" to person.otherIdentifiers.croIdentifier.inputCro))
    }
    processMessage(person)
  }

  private fun processMessage(person: Person) {
    val pncNumber = person.otherIdentifiers?.pncIdentifier!!
    val defendants = when {
      (pncNumber.valid) -> defendantRepository.findAllByPncNumber(pncNumber)
      else -> defendantRepository.findAllByFirstNameAndSurname(person.givenName, person.familyName)
    }

    val defendantMatcher = DefendantMatcher(defendants, person)
    when {
      defendantMatcher.isExactMatch() -> exactMatchFound(defendantMatcher, person)
      else -> {
        createNewDefendant(person)
      }
    }
  }

  private fun createNewDefendant(person: Person) {
    createDefendant(person)
    trackEvent(
      HMCTS_RECORD_CREATED,
      mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId),
    )
  }

  private fun createDefendant(person: Person) {
    val newDefendantEntity = DefendantEntity.from(person)

    val defendantAliases = DefendantAliasEntity.fromList(person.personAliases)
    defendantAliases.forEach { defendantAliasEntity -> defendantAliasEntity.defendant = newDefendantEntity }
    newDefendantEntity.aliases.addAll(defendantAliases)

    defendantRepository.saveAndFlush(newDefendantEntity)
  }

  private fun exactMatchFound(defendantMatcher: DefendantMatcher, person: Person) {
    val elementMap = mapOf("PNC" to person.otherIdentifiers?.pncIdentifier?.pncId, "CRN" to defendantMatcher.getMatchingItem().crn, "UUID" to person.personId.toString())
    trackEvent(HMCTS_EXACT_MATCH, elementMap)
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
