package uk.gov.justice.digital.hmpps.personrecord.jobs

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jobs.recluster.FileWaiter
import uk.gov.justice.digital.hmpps.personrecord.jobs.splitclusters.TransactionalClusterSplitService
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import java.nio.file.Path
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class SplitClustersJob(
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val transactionalClusterSplitService: TransactionalClusterSplitService,
  private val fileWaiter: FileWaiter,
  dataDir: Path = Path.of("/tmp"),
) : BatchJob {
  override val jobName = "SPLIT_CLUSTERS"
  private val path = dataDir.resolve("clusters.csv")

  override fun run() {
    val file = runBlocking {
      fileWaiter.waitFor(path)
    } ?: run {
      log.warn("File not found after waiting: {}", path)
      return
    }

    val clustersToSplit = file.toFile().readLines()

    log.info("$jobName Triggered. Number of records: ${clustersToSplit.size}.")

    clustersToSplit.forEach { personUUID ->
      val uuid = UUID.fromString(personUUID)
      val cluster = personKeyRepository.findByPersonUUID(uuid) ?: run {
        log.warn("Cluster not found for person UUID: $uuid, skipping")
        return@forEach
      }
      log.info("Splitting cluster for person UUID: ${cluster.personUUID}")

      val isClusterValidResponse = personMatchService.examineIsClusterValid(cluster)
      if (isClusterValidResponse.isClusterValid) {
        log.info("Cluster ${cluster.personUUID} is valid, not splitting")
      } else {
        log.info("Splitting ${cluster.personUUID} into ${isClusterValidResponse.clusters.size} clusters")
        log.info("Cluster ${cluster.personUUID} will keep matchIds ${isClusterValidResponse.clusters.first().map{ it }}")
        val clustersToSplitOff = isClusterValidResponse.clusters.subList(1, isClusterValidResponse.clusters.size)
        clustersToSplitOff.forEach { cluster ->
          transactionalClusterSplitService.split(cluster)
        }
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
