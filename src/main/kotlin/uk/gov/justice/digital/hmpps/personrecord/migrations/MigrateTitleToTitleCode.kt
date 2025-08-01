package uk.gov.justice.digital.hmpps.personrecord.migrations

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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.TitleCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.TitleCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateTitleToTitleCode(
  private val pseudonymRepository: PseudonymRepository,
  private val titleCodeRepository: TitleCodeRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/migrate/title-to-title-code"])
  suspend fun migrate(): String {
    migrateTitleToTitleCodes()
    return OK
  }

  suspend fun migrateTitleToTitleCodes() {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("Starting migration of title codes")
      val executionResults = forPage { page ->
        log.info("Migrating title codes, page: ${page.pageable.pageNumber + 1}")
        val pseudonyms = page.content.map { pseudonymEntity ->
          pseudonymEntity.apply {
            titleCode = lookupTitleCode(TitleCode.from(title))
          }
        }
        pseudonymRepository.saveAll(pseudonyms)
      }
      log.info(
        "Finished migrating title codes, total pages: ${executionResults.totalPages}, " +
          "total elements: ${executionResults.totalElements}, " +
          "elapsed time: ${executionResults.elapsedTime}",
      )
    }
  }

  private fun lookupTitleCode(titleCode: TitleCode?): TitleCodeEntity? = titleCode?.let { titleCodeRepository.findByCode(it.name) }

  private inline fun forPage(page: (Page<PseudonymEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var pseudonymRecords: Page<PseudonymEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)

        pseudonymRecords = pseudonymRepository.findAllByTitleNotNullOrderByIdAsc(pageable)
        page(pseudonymRecords)

        pageNumber++
      } while (pseudonymRecords.hasNext())
    }
    return ExecutionResult(
      totalPages = pseudonymRecords.totalPages,
      totalElements = pseudonymRecords.totalElements,
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
  }
}
