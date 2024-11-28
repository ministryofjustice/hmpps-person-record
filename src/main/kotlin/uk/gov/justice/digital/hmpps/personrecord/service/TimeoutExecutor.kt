package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object TimeoutExecutor {

  // Rule of thumb: Message timeout <= visibility_timeout_seconds / maxReceiveCount
  private const val MESSAGE_TIMEOUT: Long = 30000

  fun runWithTimeout(timeout: Long = MESSAGE_TIMEOUT, task: () -> Unit) = runBlocking {
    withTimeout(timeout) {
      task()
    }
  }
}
