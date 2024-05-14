package uk.gov.justice.digital.hmpps.personrecord.message.processors.delius

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.OffenderDetailRestClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.EventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED
import java.net.URI

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"
const val MAX_RETRY_ATTEMPTS: Int = 3

@Component(value = NEW_OFFENDER_CREATED)
class OffenderCreatedEventProcessor(
  val telemetryService: TelemetryService,
  val offenderDetailRestClient: OffenderDetailRestClient,
  val personService: PersonService,
  val personRepository: PersonRepository,
) : EventProcessor() {
  companion object {
    @Value("\${retry.delay}")
    private val retryDelay: Long = 0
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun processEvent(domainEvent: DomainEvent) {
    val offenderDetailUrl = domainEvent.detailUrl
    val crnIdentifier = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }
    telemetryService.trackEvent(
      DELIUS_RECORD_CREATION_RECEIVED,
      mapOf("CRN" to crnIdentifier?.value),
    )
    log.debug("Entered processEvent with  url $offenderDetailUrl")
    getNewOffenderDetail(offenderDetailUrl).fold(
      onSuccess = { deliusOffenderDetail ->
        deliusOffenderDetail?.let {
          personService.processPerson(Person.from(deliusOffenderDetail)) {
            personRepository.findAllByCrn(deliusOffenderDetail.identifiers.crn)
          }
        }
      },
      onFailure = {
        log.error("Error retrieving new offender detail: ${it.message}")
        throw it
      },
    )
  }

  private fun getNewOffenderDetail(offenderDetailsUrl: String): Result<DeliusOffenderDetail?> = runBlocking {
    try {
      return@runBlocking RetryExecutor.runWithRetry(MAX_RETRY_ATTEMPTS, retryDelay) {
        Result.success(offenderDetailRestClient.getNewOffenderDetail(URI.create(offenderDetailsUrl).path))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
