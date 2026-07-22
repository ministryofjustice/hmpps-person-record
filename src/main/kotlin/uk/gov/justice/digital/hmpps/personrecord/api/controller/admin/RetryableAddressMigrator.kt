package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.CprRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DiscardableNotFoundException

@Service
class RetryableAddressMigrator(
  private val addressService: AddressService,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val addressRepository: AddressRepository,
) {

  @CprRetryable(
    retryFor = [
      WebClientException::class,
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  fun migrateUsage(deliusAddressId: Long) {
    try {
      corePersonRecordAndDeliusClient.getAddressIgnoreNotFound(deliusAddressId)?.let {
        addressService.processAddress(
          address = it,
          findAddress = { addressRepository.findByDeliusAddressId(deliusAddressId) },
          eventSource = DELIUS,
        )
      }
    } catch (_: DiscardableNotFoundException) {
      log.info("Address with deliusAddressId $deliusAddressId not found in Delius, discarding")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
