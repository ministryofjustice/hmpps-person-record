package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class ReclusterNeedAttention(
  private val personKeyRepository: PersonKeyRepository,
  private val reclusterService: ReclusterService,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/recluster-need-attention"])
  suspend fun runJob(): String {
    reclusterNeedAttentionClusters()
    return OK
  }

  suspend fun reclusterNeedAttentionClusters() {
    CoroutineScope(Dispatchers.Default).launch {
      val executionResults = forPage { page ->
        page.content.forEach { cluster ->
          reclusterService.recluster(cluster.personEntities.first())
        }
      }
      log.info(
        JOB_NAME +
          "total elements: ${executionResults.totalElements}, " +
          "elapsed time: ${executionResults.elapsedTime}",
      )
    }
  }

  private inline fun forPage(page: (Page<PersonKeyEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var clusters: Page<PersonKeyEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)
        clusters = personKeyRepository.findAllByStatusOrderById(UUIDStatusType.NEEDS_ATTENTION, pageable)
        page(clusters)
        log.info(JOB_NAME + "${pageNumber + 1}/${clusters.totalPages}")
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
    private const val OK = "OK"
    private const val BATCH_SIZE = 100
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val JOB_NAME = "JOB: recluster-need-attention: "
  }
}
