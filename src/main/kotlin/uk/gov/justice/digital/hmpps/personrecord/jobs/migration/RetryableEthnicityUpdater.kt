package uk.gov.justice.digital.hmpps.personrecord.jobs.migration

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.DatabaseRetryable

@Component
class RetryableEthnicityUpdater(
  private val transactionalEthnicityUpdater: TransactionalEthnicityUpdater,
) {

  @DatabaseRetryable
  fun update(personId: Long) {
    transactionalEthnicityUpdater.update(personId)
  }
}
