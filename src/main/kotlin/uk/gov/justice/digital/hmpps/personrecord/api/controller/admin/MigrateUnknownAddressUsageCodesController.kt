package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateUnknownAddressUsageCodesController(
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  @Hidden
  @PostMapping(value = ["/admin/migrate-unknown-address-usage-codes"])
  suspend fun runJob(): String {
    migrate()
    return OK
  }

  suspend fun migrate() {
    CoroutineScope(Dispatchers.Default).launch {
      val executionResults = forPage { page ->
        run {
          page.content.forEach { address ->
            val deliusAddress = corePersonRecordAndDeliusClient.getAddress(address.deliusAddressId!!)
            addressService.processAddress(
              address = deliusAddress!!,
              findAddress = { address },
              eventSource = DomainEventSource.DELIUS,
            )
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

  private inline fun forPage(page: (Page<AddressEntity>) -> Unit): ExecutionResult {
    var pageNumber = 0
    var addresses: Page<AddressEntity>
    val elapsedTime: Duration = measureTime {
      do {
        val pageable = PageRequest.of(pageNumber, BATCH_SIZE)
        addresses = addressRepository.findByUsagesUsageCode(AddressUsageCode.UNKNOWN, pageable)
        page(addresses)
        log.info(JOB_NAME + "${pageNumber + 1}/${addresses.totalPages}")
        pageNumber++
      } while (addresses.hasNext())
    }
    return ExecutionResult(
      totalPages = addresses.totalPages,
      totalElements = addresses.totalElements,
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
    private const val BATCH_SIZE = 1000
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val JOB_NAME = "JOB: migrate-unknown-address-usage-codes: "
  }
}
