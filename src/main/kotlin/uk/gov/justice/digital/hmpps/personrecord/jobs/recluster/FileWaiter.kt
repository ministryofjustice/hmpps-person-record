package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import kotlinx.coroutines.delay
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.time.toKotlinDuration

@Component
class FileWaiter(
  private val properties: FileWaiterProperties,
) {

  suspend fun waitFor(
    path: Path,
  ): Path? {
    val deadline = System.nanoTime() + properties.timeout.toNanos()

    while (System.nanoTime() < deadline) {
      if (Files.exists(path)) return path
      delay(properties.pollInterval.toKotlinDuration())
    }

    return null
  }
}

@ConfigurationProperties("file-waiter")
data class FileWaiterProperties(
  val timeout: Duration = Duration.ofSeconds(120),
  val pollInterval: Duration = Duration.ofMillis(250),
)
