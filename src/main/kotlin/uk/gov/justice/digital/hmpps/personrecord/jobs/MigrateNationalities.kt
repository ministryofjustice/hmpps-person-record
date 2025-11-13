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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.NationalityRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateNationalities(
  private val nationalityRepository: NationalityRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/admin/migrate-nationalities"])
  suspend fun runJob(): String {
    migrate()
    return OK
  }

  suspend fun migrate() {
    CoroutineScope(Dispatchers.Default).launch {
      val executionResults = forPage { page ->
        run {
          val nationalities = page.content.map { nationality ->
            nationality.nationalityCode = NationalityCode.valueOf(nationality.nationalityCodeLegacy?.code!!)
            nationality
          }
          nationalityRepository.saveAll(nationalities)
        }
      }
      log.info(
        JOB_NAME +
          "total elements: ${executionResults.totalElements}, " +
          "elapsed time: ${executionResults.elapsedTime}",
      )
    }
  }

  private inline fun forPage(page: (Page<NationalityEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var nationalities: Page<NationalityEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)
        nationalities = nationalityRepository.findAllByNationalityCodeIsNull(pageable)
        page(nationalities)
        log.info(JOB_NAME + "${nationalities.totalPages}")
      } while (nationalities.hasNext())
    }
    return ExecutionResult(
      totalPages = nationalities.totalPages,
      totalElements = nationalities.totalElements,
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
