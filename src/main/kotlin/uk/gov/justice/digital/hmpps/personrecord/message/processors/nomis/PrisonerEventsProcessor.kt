package uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

const val MAX_RETRY_ATTEMPTS: Int = 3

@Service
class PrisonerEventsProcessor(
  val telemetryService: TelemetryService,
  val prisonerSearchClient: PrisonerSearchClient,
  val personService: PersonService,
  val personRepository: PersonRepository,
) {
  companion object {
    @Value("\${retry.delay}")
    private val retryDelay: Long = 0
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(domainEvent: DomainEvent) {
    val prisonNumber = domainEvent.additionalInformation?.prisonNumber!!
    telemetryService.trackEvent(
      TelemetryEventType.DOMAIN_EVENT_RECEIVED,
      mapOf(EventKeys.EVENT_TYPE to domainEvent.eventType, EventKeys.PRISON_NUMBER to prisonNumber, EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name),
    )
    getPrisonerDetails(prisonNumber).fold(
      onSuccess = {
        it?.let {
          personService.processMessage(Person.from(it), domainEvent.eventType) {
            personRepository.findByPrisonNumberAndSourceSystem(prisonNumber)
          }
        }
      },
      onFailure = {
        log.error("Error retrieving prisoner detail: ${it.message}")
        throw it
      },
    )
  }

  private fun getPrisonerDetails(nomsNumber: String): Result<Prisoner?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(prisonerSearchClient.getPrisoner(nomsNumber))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
