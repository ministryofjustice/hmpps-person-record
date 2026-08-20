package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import kotlin.time.Duration
import kotlin.time.measureTime

@Hidden
@RestController
class MigrateRecordTypesController(
  private val addressRepository: AddressRepository,
) {

  private val logger = LoggerFactory.getLogger(MigrateRecordTypesController::class.java)

  @PostMapping("/admin/migrate-record-types")
  @Transactional
  suspend fun migrate(): String {
    CoroutineScope(Dispatchers.Default).launch {
      val elapsedTime: Duration = measureTime { doMigration() }
      logger.info("Migration of record types completed in '${elapsedTime.inWholeSeconds}' seconds")
    }
    return "OK"
  }

  private suspend fun doMigration() {
    val allCommonPlatformAddressWithNullStatusCode = addressRepository.findAllByCommonPlatformByNullStatusCode()
    logger.info("Found '${allCommonPlatformAddressWithNullStatusCode.size}' COMMON_PLATFORM addresses with a null status code")
    val cpAddressByPersonIdentifier = allCommonPlatformAddressWithNullStatusCode.groupBy { it.person!!.id!! }

    cpAddressByPersonIdentifier.forEach { (_, addressesForPerson) ->
      addressesForPerson.forEach { address ->
        when (address.recordType) {
          AddressRecordType.PRIMARY -> {
            address.statusCode = AddressStatusCode.M
          }
          AddressRecordType.PREVIOUS -> {
            address.statusCode = AddressStatusCode.P
          }
          null -> {
            val addressesSortedByAddressId = addressesForPerson.sortedBy { it.id }
            val latestAddress = addressesSortedByAddressId.last()
            latestAddress.statusCode = AddressStatusCode.M

            addressesSortedByAddressId
              .filter { it.id != latestAddress.id }
              .onEach { it.statusCode = AddressStatusCode.P }
          }
        }
      }
      addressRepository.saveAll(addressesForPerson)
    }
  }
}
