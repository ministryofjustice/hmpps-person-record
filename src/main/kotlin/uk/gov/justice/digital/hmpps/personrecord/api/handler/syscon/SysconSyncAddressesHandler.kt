package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.util.UUID

@Component
class SysconSyncAddressesHandler(
  private val addressService: AddressService,
  private val addressRepository: AddressRepository,
  private val personRepository: PersonRepository,
) {
  @Transactional
  fun handleInsert(prisonNumber: String, prisonAddress: PrisonAddress): SysconAddressMapping {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
    val addressEntity = addressService.processAddress(
      address = prisonAddress.toAddress(),
      findPerson = { personEntity },
      findAddress = { null },
      eventSource = DomainEventSource.NOMIS,
    )
    return SysconAddressMapping(
      nomisAddressId = prisonAddress.nomisAddressId,
      cprAddressId = addressEntity.updateId.toString(),
    )
  }

  @Transactional
  fun handleDelete(cprAddressId: String) {
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressId))
    addressService.deleteAddress(
      findAddress = { addressEntity },
      eventSource = DomainEventSource.NOMIS,
    )
  }

  companion object {
    fun PrisonAddress.toAddress() = Address(
      startDate = startDate?.toUkZonedDateTime(),
      endDate = endDate?.toUkZonedDateTime(),
      noFixedAbode = noFixedAbode,
      fullAddress = fullAddress,
      postcode = postcode,
      subBuildingName = subBuildingName,
      buildingName = buildingName,
      buildingNumber = buildingNumber,
      thoroughfareName = thoroughfareName,
      dependentLocality = dependentLocality,
      postTown = postTown,
      county = county,
      countryCode = countryCode,
      comment = comment,
      statusCode = AddressStatusCode.fromPrison(isPrimary, isMail ?: false),
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime,
      createUserId = createUserId,
    )
  }
}
