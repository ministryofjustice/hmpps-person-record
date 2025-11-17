package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.TransactionalReclusterService
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class CalculateStatusReason(
  private val personKeyRepository: PersonKeyRepository,
  private val transactionalReclusterService: TransactionalReclusterService,
) {

  @Hidden
  @PostMapping("/admin/calculate-status-reason")
  suspend fun postRecluster(): String {
    CoroutineScope(Dispatchers.Default).launch {
      forPage { clusters ->
        clusters.forEach {
          it.setAsActive()
          val activeCluster = personKeyRepository.save(it)
          transactionalReclusterService.recluster(activeCluster.personEntities.first())
        }
      }
    }
    return OK
  }

  private inline fun forPage(page: (Page<PersonKeyEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var clusters: Page<PersonKeyEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)
        clusters = personKeyRepository.findAllByStatusAndStatusReasonIsNull(UUIDStatusType.NEEDS_ATTENTION, pageable)
        page(clusters)
        pageNumber++
        log.info(JOB_NAME + "${clusters.totalPages}")
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
    private const val BATCH_SIZE = 500
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val JOB_NAME = "JOB: migrate-nationalities: "
  }
}
