package uk.gov.justice.digital.hmpps.personrecord.migrations

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EthnicityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateEthnicityCode(
  private val ethnicityCodeRepository: EthnicityCodeRepository,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/migrate/ethnicity-codes"])
  suspend fun migrate(): String {
    migrateEthnicityCodes()
    return OK
  }

  suspend fun migrateEthnicityCodes() = coroutineScope {
    log.info("Starting migration of ethnicity codes")
    val preLoadedEthnicityCodes: Map<String, EthnicityCodeEntity> = ethnicityCodeRepository.findAll().associateBy { it.code }
    val executionResults = forPage { page ->
      log.info("Migrating ethnicity codes, page: ${page.pageable.pageNumber + 1}")
      page.content.map { personEntity ->
        val ethnicityCode = personEntity.getEthnicityCode()
        val ethnicityCodeEntity = ethnicityCode?.let { preLoadedEthnicityCodes[it.name] }

        if (ethnicityCodeEntity != null) {
          personEntity.ethnicityCode = ethnicityCodeEntity
          personRepository.save(personEntity)
        }
      }
    }
    log.info(
      "Finished migrating ethnicity codes, total pages: ${executionResults.totalPages}, " +
        "total elements: ${executionResults.totalElements}, " +
        "elapsed time: ${executionResults.elapsedTime}",
    )
  }

  private fun PersonEntity.getEthnicityCode(): EthnicityCode? = EthnicityCode.from(this.ethnicity)

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var personRecords: Page<PersonEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)

        personRecords = personRepository.findAllByEthnicityIsNotNull(pageable)
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
