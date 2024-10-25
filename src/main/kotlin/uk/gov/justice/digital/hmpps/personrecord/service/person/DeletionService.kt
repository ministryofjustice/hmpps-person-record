package uk.gov.justice.digital.hmpps.personrecord.service.person

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class DeletionService (
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {
  fun processDelete(personCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      personCallback()?.let {
        handleDeletion(it)
      }
    }
  }

  private fun handleDeletion(personEntity: PersonEntity) {
    deletePersonRecord(personEntity)
  }

  private fun deletePersonRecord(personEntity: PersonEntity) {
    personRepository.delete(personEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_DELETED,
      personEntity,
      mapOf(EventKeys.UUID to personEntity.personKey?.personId.toString())
    )
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    personEntity: PersonEntity,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to personEntity.sourceSystem.name,
      EventKeys.DEFENDANT_ID to personEntity.defendantId,
      EventKeys.CRN to personEntity.crn,
      EventKeys.PRISON_NUMBER to personEntity.prisonNumber,
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }

  companion object {
    const val MAX_ATTEMPTS: Int = 5
  }
}