package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import kotlin.time.measureTime

@Hidden
@RestController
class MigrateRecordTypesController(
  private val addressRepository: AddressRepository,
) {

  private val logger = LoggerFactory.getLogger(MigrateRecordTypesController::class.java)

  @PostMapping("/admin/migrate-record-types")
  suspend fun migrate(): String {
    CoroutineScope(Dispatchers.Default).launch {
      val elapsedTime = measureTime { runMigration() }
      logger.info("Full migration time of record types completed in '${elapsedTime.inWholeSeconds}' seconds")
    }
    return "OK"
  }

  private suspend fun runMigration() {
    var lastId = 0L
    while (true) {
      val batchCPAddressWithNullStatusCode = addressRepository.findByIdGreaterThanAndStatusCodeIsNullOrderedByIdAsc(1000, lastId)
      if (batchCPAddressWithNullStatusCode.isEmpty()) {
        break
      }
      val elapsedTime = measureTime {
        migrateBatch(batchCPAddressWithNullStatusCode)
      }
      logger.info("Batch migration of record types completed in '${elapsedTime.inWholeSeconds}' seconds")
      lastId = batchCPAddressWithNullStatusCode.last().id!!
    }
  }

  @Transactional
  private fun migrateBatch(batchOfAddresses: List<AddressEntity>) {
    val addressForPerson = batchOfAddresses.groupBy { it.person!!.id!! }

    addressForPerson.forEach { (_, addressesForPerson) ->
      val latestAddress = addressesForPerson.last()
      latestAddress.statusCode = AddressStatusCode.M

      addressesForPerson.forEach {
        if (it.id != latestAddress.id) {
          it.statusCode = AddressStatusCode.P
        }
      }
      addressRepository.saveAll(addressesForPerson)
    }
  }
}
