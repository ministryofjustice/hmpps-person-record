package uk.gov.justice.digital.hmpps.personrecord.service.person

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_RECLUSTER_NEEDS_ATTENTION

@Component
class ReclusterService(
  private val telemetryService: TelemetryService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun recluster(personEntity: PersonEntity) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      handleRecluster(personEntity)
    }
  }

  private fun handleRecluster(personEntity: PersonEntity) {
    when {
      clusterNeedsAttention(personEntity.personKey) -> telemetryService.trackPersonEvent(
        CPR_UUID_RECLUSTER_NEEDS_ATTENTION,
        personEntity,
        mapOf(
          EventKeys.UUID to personEntity.personKey?.personId.toString(),
        ),
      )
      clusterHasOneRecord(personEntity.personKey) -> {} // CPR-437
      else -> {} // CPR-439
    }
  }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION

  private fun clusterHasOneRecord(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.personEntities?.size == 1

  companion object {
    private const val MAX_ATTEMPTS = 5
  }
}
