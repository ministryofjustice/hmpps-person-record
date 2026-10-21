package uk.gov.justice.digital.hmpps.personrecord.jobs

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.FileWaiter
import java.nio.file.Path

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
@Profile("!preprod & !prod")
class SplitClustersJob(

  private val jsonMapper: JsonMapper,
  private val fileWaiter: FileWaiter,
  dataDir: Path = Path.of("/tmp"),
) : BatchJob {
  override val jobName = "SPLIT_CLUSTERS"
  private val path = dataDir.resolve("recluster.json")

  override fun run() {
    val file = runBlocking {
      fileWaiter.waitFor(path)
    } ?: run {
      log.warn("File not found after waiting: {}", path)
      return
    }
    val splitClusterRecords = file.toFile().readLines()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val SPLIT_PROCESS_NAME = "Split Clusters"
  }
}
