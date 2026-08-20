package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
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
      val batchOfCommonPlatformPersons = personRepository.findByIdGreaterThanAndSourceSystemIsCommonPlatformOrderedByIdAsc(migrationRequest.batchSize, lastPersonId)
      if (batchOfCommonPlatformPersons.isEmpty()) {
        break
      }
      val elapsedTime = measureTime {
        migrateBatch(batchOfCommonPlatformPersons)
      }
      logger.info("Batch migration of '${migrationRequest.batchSize}' completed in '${elapsedTime.inWholeSeconds}' seconds")
      lastPersonId = batchOfCommonPlatformPersons.last().id!!
    }
  }

  @Transactional
  private fun migrateBatch(batchOfPersons: List<PersonEntity>) {
    batchOfPersons.forEach { personEntity ->
      val personAddressSorted = personEntity.addresses.sortedBy { it.id }
      val latestAddressEntity = personAddressSorted.last()
      latestAddressEntity.statusCode = AddressStatusCode.M

      personAddressSorted.forEach { addressEntity ->
        if (addressEntity.id != latestAddressEntity.id) {
          addressEntity.statusCode = AddressStatusCode.P
        }
      }
    }
    personRepository.saveAll(batchOfPersons)
  }

  data class RecordTypeMigrationDetails(
    val batchSize: Int,
  )
}
