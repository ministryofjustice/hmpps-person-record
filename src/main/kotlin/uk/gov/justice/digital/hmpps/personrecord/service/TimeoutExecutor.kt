package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object TimeoutExecutor {
  // 90 secs, SQS visibility is set to 120 secs
  private const val DEFAULT_TIMEOUT: Long = 90000

  fun runWithTimeout(timeout: Long = DEFAULT_TIMEOUT, task: () -> Unit) = runBlocking {
    withTimeout(timeout) {
      task()
    }
  }
}
