package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import kotlin.time.measureTime

@Hidden
@RestController
class MigrateRecordTypesController(
  private val personRepository: PersonRepository,
) {

  private val logger = LoggerFactory.getLogger(MigrateRecordTypesController::class.java)

  @PostMapping("/admin/migrate-record-types")
  suspend fun migrate(@RequestBody migrationRequest: RecordTypeMigrationDetails): String {
    CoroutineScope(Dispatchers.Default).launch {
      val elapsedTime = measureTime { runMigration(migrationRequest) }
      logger.info("Full migration time of record types completed in '${elapsedTime.inWholeSeconds}' seconds")
    }
    return "OK"
  }

  private suspend fun runMigration(migrationRequest: RecordTypeMigrationDetails) {
    var lastPersonId = 0L
    while (true) {
      val batchOfCommonPlatformPersons = personRepository.findByIdGreaterThanAndSourceSystemOrderByIdAsc(
        id = lastPersonId,
        sourceSystem = SourceSystemType.COMMON_PLATFORM,
        limit = Limit.of(migrationRequest.personBatchSize),
      )
      if (batchOfCommonPlatformPersons.isEmpty()) {
        break
      }
      val elapsedTime = measureTime {
        migrateBatch(batchOfCommonPlatformPersons)
      }
      logger.info("Batch migration of '${migrationRequest.personBatchSize}' completed in '${elapsedTime.inWholeSeconds}' seconds")
      lastPersonId = batchOfCommonPlatformPersons.last().id!!
    }
  }

  @Transactional
  private fun migrateBatch(batchOfPersons: List<PersonEntity>) {
    batchOfPersons.forEach { personEntity ->
      val addressesWithNoStatusCode = personEntity.addresses.filter { it.statusCode == null }
      if (addressesWithNoStatusCode.isEmpty()) return@forEach
      addressesWithNoStatusCode.forEach { addressEntity ->
        when (addressEntity.recordType) {
          AddressRecordType.PRIMARY, null -> addressEntity.statusCode = AddressStatusCode.M
          AddressRecordType.PREVIOUS -> addressEntity.statusCode = AddressStatusCode.P
        }
      }
    }
    personRepository.saveAll(batchOfPersons)
  }

  data class RecordTypeMigrationDetails(
    val personBatchSize: Int,
  )
}
