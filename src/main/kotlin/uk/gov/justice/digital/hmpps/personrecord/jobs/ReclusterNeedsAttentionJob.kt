package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import kotlin.time.Duration
import kotlin.time.measureTime

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class ReclusterNeedsAttentionJob(
  private val personKeyRepository: PersonKeyRepository,
  private val transactionalReclusterService: TransactionalReclusterService,
) : BatchJob {
  override val jobName = "RECLUSTER_NEEDS_ATTENTION"

  override fun run() {
    val executionResults = forPage { page ->
      page.content.forEach { cluster ->
        transactionalReclusterService.recluster(cluster.personEntities.first())
      }
    }
    log.info(jobName + " total elements: ${executionResults.totalElements}, " + "elapsed time: ${executionResults.elapsedTime}")
  }

  private inline fun forPage(page: (Page<PersonKeyEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var clusters: Page<PersonKeyEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)
        clusters = personKeyRepository.findAllByStatusOrderById(UUIDStatusType.NEEDS_ATTENTION, pageable)
        page(clusters)
        log.info(jobName + " ${pageNumber + 1}/${clusters.totalPages}")
        pageNumber++
      } while (clusters.hasNext())
    }
    return ExecutionResult(
      totalPages = clusters.totalPages,
      totalElements = clusters.totalElements,
      elapsedTime = elapsedTime,
    )
  }

  private data class ExecutionResult(
    val totalPages: Int,
    val totalElements: Long,
    val elapsedTime: Duration,
  )

  companion object {
    private const val BATCH_SIZE = 100
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
