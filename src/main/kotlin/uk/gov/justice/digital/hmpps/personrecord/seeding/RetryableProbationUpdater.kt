package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DiscardableNotFoundException

@Component
class RetryableProbationUpdater(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val probationEventProcessor: ProbationEventProcessor,
) {

  @Retryable(
    maxAttempts = 2,
    backoff = Backoff(delay = 200, random = true, multiplier = 2.0),
    retryFor = [
      WebClientException::class,
    ],
  )
  fun repopulateProbationRecord(entity: PersonEntity) {
    try {
      corePersonRecordAndDeliusClient.getPersonErrorIfNotFound(entity.crn!!)?.let { person ->
        probationEventProcessor.processEvent(person)
      }
    } catch (e: DiscardableNotFoundException) {
      log.info("Discarded probation record for CRN: ${entity.crn}. Reason: ${e.message}", e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
