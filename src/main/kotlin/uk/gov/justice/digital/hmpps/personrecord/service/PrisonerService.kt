package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonServiceClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PrisonerMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerDetails
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.matcher.PrisonerMatcher
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PrisonerService(
  private val telemetryService: TelemetryService,
  private val personRecordService: PersonRecordService,
  private val client: PrisonerSearchClient,
  private val featureFlag: FeatureFlag,
  val prisonServiceClient: PrisonServiceClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val NO_RECORDS_MESSAGE = "No Nomis matching records exist"
    const val EXACT_MATCH_MESSAGE = "Exact Nomis match found - adding prisoner to person record"
    const val PARTIAL_MATCH_MESSAGE = "Partial Nomis match found"
    const val PRISONER_DETAILS_NOT_FOUND_MESSAGE = "No prisoner details returned from Nomis"
    const val PRISONER_DETAILS_FOUND_MESSAGE = "Prisoner details returned from Nomis"
    val exceptionsToRetryOn = listOf(HttpClientErrorException::class, HttpServerErrorException::class)
  }

  fun processAssociatedPrisoners(personEntity: PersonEntity, person: Person) {
    log.debug("Entered processAssociatedPrisoners")
    if (featureFlag.isNomisSearchEnabled()) {
      val prisonerMatcher = getPrisonerMatcher(person)
      when {
        prisonerMatcher.isPncDoesNotMatch() -> pncDoesNotMatch(prisonerMatcher, personEntity, person)
        prisonerMatcher.isExactMatch() -> exactMatchFound(prisonerMatcher, personEntity, person)
        prisonerMatcher.isPartialMatch() -> partialMatchFound(personEntity, person)
        else -> noRecordsFound(personEntity, person)
      }
    }
  }

  private fun pncDoesNotMatch(prisonerMatcher: PrisonerMatcher, personEntity: PersonEntity, person: Person) {
    prisonerMatcher.items?.let { trackPncMismatchEvent(it, personEntity, person) }
  }

  private fun noRecordsFound(personEntity: PersonEntity, person: Person) {
    logAndTrackEvent(NO_RECORDS_MESSAGE, TelemetryEventType.NOMIS_NO_MATCH_FOUND, personEntity, person)
  }

  private fun partialMatchFound(personEntity: PersonEntity, person: Person) {
    logAndTrackEvent(PARTIAL_MATCH_MESSAGE, TelemetryEventType.NOMIS_PARTIAL_MATCH_FOUND, personEntity, person)
  }

  private fun exactMatchFound(prisonerMatcher: PrisonerMatcher, personEntity: PersonEntity, person: Person) {
    val matchingPrisoner = prisonerMatcher.getMatchingItem()
    logAndTrackEvent(EXACT_MATCH_MESSAGE, TelemetryEventType.NOMIS_MATCH_FOUND, personEntity, person)
    val prisonerDetails = getPrisonerDetails(matchingPrisoner.prisonerNumber)
    if (prisonerDetails != null) {
      handlePrisonerDetailsFound(personEntity, person, prisonerDetails)
    } else {
      logAndTrackEvent(
        PRISONER_DETAILS_NOT_FOUND_MESSAGE,
        TelemetryEventType.NOMIS_PRISONER_DETAILS_NOT_FOUND,
        personEntity,
        person,
      )
    }
  }

  private fun handlePrisonerDetailsFound(personEntity: PersonEntity, person: Person, prisonerDetails: PrisonerDetails) {
    logAndTrackEvent(
      PRISONER_DETAILS_FOUND_MESSAGE,
      TelemetryEventType.NOMIS_PRISONER_DETAILS_FOUND,
      personEntity,
      person,
    )
    personRecordService.addPrisonerToPerson(personEntity, prisonerDetails)
  }

  private fun getPrisonerDetails(prisonerNumber: String): PrisonerDetails? = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(exceptionsToRetryOn, 3) {
        val prisonerDetails = prisonServiceClient.getPrisonerDetails(prisonerNumber)
        prisonerDetails?.let {
          updatePrisonerAddresses(prisonerNumber, prisonerDetails)
        }
        prisonerDetails
      }
    } catch (exception: Exception) {
      telemetryService.trackEvent(TelemetryEventType.NOMIS_CALL_FAILED, mapOf("Prisoner Number" to prisonerNumber))
      throw exception
    }
  }

  private fun updatePrisonerAddresses(prisonerNumber: String, prisonerDetails: PrisonerDetails) = runBlocking {
    return@runBlocking RetryExecutor.runWithRetry(exceptionsToRetryOn, 3) {
      val prisonerAddress = prisonServiceClient.getPrisonerAddresses(prisonerNumber)
      if (prisonerAddress?.isNotEmpty() == true) {
        prisonerDetails.addresses = prisonerAddress
      }
    }
  }

  private fun getPrisonerMatcher(person: Person): PrisonerMatcher = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(exceptionsToRetryOn, 3) { findMatchingPrisonersAndCreateMatcher(person) }
    } catch (exception: Exception) {
      telemetryService.trackEvent(
        TelemetryEventType.NOMIS_CALL_FAILED,
        mapOf("Prisoner Number" to person.otherIdentifiers?.prisonNumber),
      )
      throw exception
    }
  }

  private fun findMatchingPrisonersAndCreateMatcher(person: Person): PrisonerMatcher {
    val prisoners = client.findPossibleMatches(PrisonerMatchCriteria.from(person))
    val pncNumbersReturned = prisoners?.joinToString(" ") { it.pncNumber.toString() }
    log.debug("Number of prisoners returned from Nomis for PNC ${person.otherIdentifiers?.pncIdentifier} = ${prisoners?.size ?: 0} having PNCs: $pncNumbersReturned")
    return PrisonerMatcher(prisoners, person)
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
        "Prisoner Number" to person.otherIdentifiers?.prisonNumber,
      ).filterValues { !it.isNullOrBlank() },
    )
  }

  private fun trackPncMismatchEvent(
    prisonerList: List<Prisoner>,
    personEntity: PersonEntity,
    person: Person,
  ) {
    telemetryService.trackEvent(
      TelemetryEventType.NOMIS_PNC_MISMATCH,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC searched for" to person.otherIdentifiers?.pncIdentifier.toString(),
        "PNC returned from search" to prisonerList.joinToString(" ") { it.pncNumber.toString() },
        "Prisoner Number" to prisonerList.singleOrNull()?.prisonerNumber,
      ).filterValues { !it.isNullOrBlank() },
    )
  }
}
