package uk.gov.justice.digital.hmpps.personrecord.message.processors.delius

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED

const val MAX_RETRY_ATTEMPTS: Int = 3

@Component
class OffenderEventProcessor(
  val telemetryService: TelemetryService,
  val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  val personService: PersonService,
  val personRepository: PersonRepository,
) {
  companion object {
    @Value("\${retry.delay}")
    private val retryDelay: Long = 0
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(domainEvent: DomainEvent) {
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }!!.value
    telemetryService.trackEvent(
      DOMAIN_EVENT_RECEIVED,
      mapOf(EventKeys.CRN to crn, EventKeys.EVENT_TYPE to domainEvent.eventType, EventKeys.SOURCE_SYSTEM to SourceSystemType.DELIUS.name),
    )
    getProbationCase(crn).fold(
      onSuccess = {
        it?.let {
          personService.processMessage(Person.from(it), domainEvent.eventType) {
            personRepository.findByCrn(crn)
          }
        }
      },
      onFailure = {
        log.error("Error retrieving new offender detail: ${it.message}")
        throw it
      },
    )
  }

  private fun getProbationCase(crn: String): Result<ProbationCase?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(corePersonRecordAndDeliusClient.getProbationCase(crn))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
