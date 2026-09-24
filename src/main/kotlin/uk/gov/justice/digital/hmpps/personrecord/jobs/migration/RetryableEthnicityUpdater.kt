package uk.gov.justice.digital.hmpps.personrecord.jobs.migration

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.DatabaseRetryable
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Component
class RetryableEthnicityUpdater(
  private val transactionalEthnicityUpdater: TransactionalEthnicityUpdater,
) {

  @DatabaseRetryable
  fun update(person: PersonEntity) {
    transactionalEthnicityUpdater.update(person)
  }
}
