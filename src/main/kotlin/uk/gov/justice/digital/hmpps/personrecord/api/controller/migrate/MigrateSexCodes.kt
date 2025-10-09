package uk.gov.justice.digital.hmpps.personrecord.api.controller.migrate

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateSexCodes(
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/migrate/sex-codes"])
  suspend fun migrateSexCodes(): String {
    migrateSexCodesFromPersonToPseudonym()
    return OK
  }

  suspend fun migrateSexCodesFromPersonToPseudonym() = coroutineScope {
    log.info("Starting migration of sex codes")
    val executionResults = forPage { page ->
      page.content.forEach { personEntity ->
        personEntity.getPrimaryName().sexCode = personEntity.sexCode
        personRepository.save(personEntity)
      }
    }
    log.info(
      "Finished migrating exclude override markers," +
        "total elements: ${executionResults.totalElements}, " +
        "elapsed time: ${executionResults.elapsedTime}",
    )
  }

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var personRecords: Page<PersonEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)

        personRecords = personRepository.findAllBySexCodeIsNotNullAndSourceSystemOrderById(pageable, SourceSystemType.COMMON_PLATFORM)
        page(personRecords)

        pageNumber++
      } while (personRecords.hasNext())
    }
    return ExecutionResult(
      totalPages = personRecords.totalPages,
      totalElements = personRecords.totalElements,
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