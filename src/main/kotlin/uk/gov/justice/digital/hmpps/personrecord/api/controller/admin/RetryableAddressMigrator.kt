package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import jakarta.persistence.OptimisticLockException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.CprRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService

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
    val deliusAddress = corePersonRecordAndDeliusClient.getAddress(deliusAddressId)
    addressService.processAddress(
      address = deliusAddress!!,
      findAddress = { addressRepository.findByDeliusAddressId(deliusAddressId) },
      eventSource = DELIUS,
    )
  }
}
