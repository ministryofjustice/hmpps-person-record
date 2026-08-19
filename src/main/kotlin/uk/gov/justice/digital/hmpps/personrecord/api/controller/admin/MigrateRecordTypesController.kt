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
    // paginate
    val allCommonPlatformAddressWithARecordType = addressRepository.findAllByCommonPlatform()
    logger.info("Found '${allCommonPlatformAddressWithARecordType.size}' COMMON_PLATFORM addresses")
  }
}
