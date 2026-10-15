package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCreateAddressRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.Companion.MAIL_TYPES
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.Companion.PRIMARY_TYPES
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.Companion.removeFlags
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.time.LocalDateTime

@Component
class PrisonAddressesHandler(
  private val personRepository: PersonRepository,
  private val addressService: AddressService,
) {
  @Transactional(readOnly = true)
  fun get(prisonNumber: String): List<CanonicalAddress> = findPerson(prisonNumber).addresses
    // same sort order as was in Prison API previously
    .sortedWith(compareByDescending(nullsFirst()) { it.startDate })
    .map { CanonicalAddress.from(it) }

  @Transactional
  fun create(prisonNumber: String, request: PrisonCreateAddressRequest): CanonicalAddress {
    val person = findPerson(prisonNumber)

    val newAddressIsPrimary = request.statusCode in PRIMARY_TYPES
    val newAddressIsMail = request.statusCode in MAIL_TYPES

    // Mirrors Prison API: existing addresses lose the primary/mail flags taken by the new address.
    // Updated in place so no AddressUpdated event is raised for them.
    person.addresses.forEach { existingAddress ->
      val newStatusCode = existingAddress.statusCode.removeFlags(newAddressIsPrimary, newAddressIsMail)
      if (newStatusCode != existingAddress.statusCode) {
        existingAddress.statusCode = newStatusCode
        existingAddress.modifyUserId = request.userId
        existingAddress.modifyDateTime = LocalDateTime.now()
      }
    }

    val addressEntity = addressService.create(
      address = Address.from(request),
      findPerson = { person },
      eventSource = CPR,
    )
    return CanonicalAddress.from(addressEntity)
  }

  private fun findPerson(prisonNumber: String): PersonEntity = personRepository.findByPrisonNumber(prisonNumber)
    ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
}
