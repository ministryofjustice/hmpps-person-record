package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateEthnicityCode(
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/admin/migrate-ethnicity-codes"])
  suspend fun runJob(): String {
    migrate()
    return OK
  }

  suspend fun migrate() {
    CoroutineScope(Dispatchers.Default).launch {
      val executionResults = forPage { page ->
        run {
          page.content.forEach { person ->
            person.ethnicityCode = EthnicityCode.valueOf(person.ethnicityCodeLegacy!!.code)
            personRepository.save(person)
          }
        }
      }
      log.info(
        JOB_NAME +
          "total elements: ${executionResults.totalElements}, " +
          "elapsed time: ${executionResults.elapsedTime}",
      )
    }
  }

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var personEntities: Page<PersonEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)
        personEntities = personRepository.findAllByEthnicityCodeLegacyIsNotNullAndEthnicityCodeIsNull(pageable)
        page(personEntities)
        log.info(JOB_NAME + "${personEntities.totalPages}")
      } while (personEntities.hasNext())
    }
    return ExecutionResult(
      totalPages = personEntities.totalPages,
      totalElements = personEntities.totalElements,
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
    private const val JOB_NAME = "JOB: migrate-ethnicity-code: "
  }
}
