package uk.gov.justice.digital.hmpps.personrecord.seeding

import jakarta.persistence.OptimisticLockException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.CprRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams

@Component
class RetryableProbationUpdater(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val transactionalProbationUpdater: TransactionalProbationUpdater,
) {

  @CprRetryable(
    retryFor = [
      WebClientException::class,
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  fun repopulateProbationRecord(pageParams: CorePersonRecordAndDeliusClientPageParams) {
    corePersonRecordAndDeliusClient.getProbationCases(pageParams)
      ?.cases?.forEach {
        transactionalProbationUpdater.update(it)
      }
  }
}
