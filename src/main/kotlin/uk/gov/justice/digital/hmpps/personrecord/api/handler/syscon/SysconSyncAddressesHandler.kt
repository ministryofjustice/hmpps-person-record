package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.MA
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.PM
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
    val addressEntity = addressService.create(
      address = prisonAddress.toAddress(),
      findPerson = { personEntity },
      eventSource = DomainEventSource.NOMIS,
    )
    return SysconAddressMapping(
      nomisAddressId = prisonAddress.nomisAddressId!!,
      cprAddressId = addressEntity.updateId.toString(),
    )
  }

  @Transactional
  fun handleDelete(cprAddressId: String) {
    addressService.deleteAddress(
      findAddress = { addressRepository.findByUpdateId(UUID.fromString(cprAddressId)) },
      eventSource = DomainEventSource.NOMIS,
    )
  }

  fun handleGet(prisonNumber: String, cprAddressId: String): PrisonAddress {
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressId))
      ?: throw ResourceNotFoundException("Address with $cprAddressId not found for person with $prisonNumber")
    return addressEntity.toPrisonAddress()
  }

  companion object {

    fun isPrimary(addressStatusCode: AddressStatusCode?): Boolean = when (addressStatusCode) {
      M, PM -> true
      else -> false
    }

    fun isMail(addressStatusCode: AddressStatusCode?): Boolean = when (addressStatusCode) {
      PM, MA -> true
      else -> false
    }

    fun AddressEntity.toPrisonAddress() = PrisonAddress(
      startDate = startDate?.toUkLocalDate(),
      endDate = endDate?.toUkLocalDate(),
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
      isPrimary = isPrimary(statusCode),
      isMail = isMail(statusCode),
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime!!,
      createUserId = createUserId!!,
    )

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
