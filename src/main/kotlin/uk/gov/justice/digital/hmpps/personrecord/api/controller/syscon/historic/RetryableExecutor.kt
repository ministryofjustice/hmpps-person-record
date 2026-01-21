package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.service.TimeoutExecutor

@Component
class RetryableExecutor {

  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun exec(action: () -> Unit) = TimeoutExecutor.runWithTimeout {
    action()
  }
}
