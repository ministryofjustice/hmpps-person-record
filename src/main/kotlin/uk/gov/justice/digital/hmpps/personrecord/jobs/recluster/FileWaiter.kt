package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Component
class FileWaiter {

  suspend fun waitFor(
    path: Path,
    timeout: Duration = 60.seconds,
    pollInterval: Duration = 250.milliseconds,
  ): Path? {
    val deadline = System.nanoTime() + timeout.inWholeNanoseconds

    while (System.nanoTime() < deadline) {
      if (Files.exists(path)) return path
      delay(pollInterval)
    }

    return null
  }
}
