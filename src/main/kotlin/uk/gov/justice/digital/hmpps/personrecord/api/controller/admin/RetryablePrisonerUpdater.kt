package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor

@Component
class RetryablePrisonerUpdater(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonEventProcessor: PrisonEventProcessor,
) {

  @Retryable(
    maxAttempts = 2,
    backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
    retryFor = [
      WebClientException::class,
    ],
  )
  fun repopulatePrisonRecord(entity: PersonEntity) {
    prisonerSearchClient.getPrisoner(entity.prisonNumber!!)?.let { person ->
      prisonEventProcessor.processEvent(person)
    }
  }
}
