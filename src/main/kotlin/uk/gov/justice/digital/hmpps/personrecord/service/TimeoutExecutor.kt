package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object TimeoutExecutor {

  fun runWithTimeout(timeout: Long, task: () -> Unit) = runBlocking {
    withTimeout(timeout) {
      task()
    }
  }
}
