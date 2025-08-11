package uk.gov.justice.digital.hmpps.personrecord.migrations

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.NationalityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.NationalityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Nationality
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateNationalityCode(
  private val nationalityCodeRepository: NationalityCodeRepository,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/migrate/nationality-codes"])
  suspend fun migrate(): String {
    migrateNationalityCodes()
    return OK
  }

  suspend fun migrateNationalityCodes() = coroutineScope {
    log.info("Starting migration of nationality codes")
    val preLoadedNationalityCodes: List<NationalityCodeEntity> = nationalityCodeRepository.findAll()
    val executionResults = forPage { page ->
      log.info("Migrating nationality codes, page: ${page.pageable.pageNumber + 1}")
      page.content.map { personEntity ->
        val nationalityCode = personEntity.getNationalityCodeForSourceSystem()
        val nationalityCodeEntity = preLoadedNationalityCodes.lookupTitleCode(nationalityCode)
        NationalityEntity.from(Nationality(nationalityCode), nationalityCodeEntity)?.let {
          personEntity.nationalities.clear()
          it.person = personEntity
          personEntity.nationalities.add(it)
        }
        personRepository.save(personEntity)
      }
    }
    log.info(
      "Finished migrating nationality codes, total pages: ${executionResults.totalPages}, " +
        "total elements: ${executionResults.totalElements}, " +
        "elapsed time: ${executionResults.elapsedTime}",
    )
  }

  private fun PersonEntity.getNationalityCodeForSourceSystem(): NationalityCode? = when (this.sourceSystem) {
    SourceSystemType.DELIUS -> NationalityCode.fromProbationMapping(this.nationality)
    SourceSystemType.NOMIS -> NationalityCode.fromPrisonMapping(this.nationality)
    SourceSystemType.LIBRA -> NationalityCode.fromLibraMapping(this.nationality)
    SourceSystemType.COMMON_PLATFORM -> NationalityCode.fromCommonPlatformMapping(this.nationality)
    else -> null
  }

  private fun List<NationalityCodeEntity>.lookupTitleCode(nationalityCode: NationalityCode?): NationalityCodeEntity? = nationalityCode?.let { this.find { it.code == nationalityCode.name } }

  private inline fun forPage(page: (Page<PersonEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var personRecords: Page<PersonEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)

        personRecords = personRepository.findAllByNationalityIsNotNull(pageable)
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
