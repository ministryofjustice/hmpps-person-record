package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Component
class SysconSyncAddressUsagesHandler(
  private val addressUsageRepository: AddressUsageRepository,
  private val addressRepository: AddressRepository,
) {

  @Transactional
  fun handleInsert(prisonNumber: String, cprAddressId: String, prisonAddressUsage: PrisonAddressUsage): SysconAddressUsageMapping {
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressId))
      ?: throw ResourceNotFoundException("Address with $cprAddressId not found for person with $prisonNumber")
    val addressUsageEntity = addressUsageRepository.saveAndFlush(prisonAddressUsage.toEntity(addressEntity))
    addressEntity.usages.add(addressUsageEntity)
    return (prisonAddressUsage to addressUsageEntity).toMapping()
  }

  @Transactional
  fun handleUpdate(prisonNumber: String, cprAddressId: String, cprUsageId: String, prisonAddressUsage: PrisonAddressUsage) {
    val addressUsageEntity = addressUsageRepository.findByUpdateId(UUID.fromString(cprUsageId))
      ?: throw ResourceNotFoundException("AddressUsage with $cprUsageId not found for address with $cprAddressId with person with $prisonNumber")
    addressUsageEntity.updateFrom(prisonAddressUsage)
  }

  @Transactional
  fun handleDelete(prisonNumber: String, cprAddressId: String, cprUsageId: String) {
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressId))
      ?: throw ResourceNotFoundException("Address with $cprAddressId not found for person with $prisonNumber")
    addressEntity.usages.removeIf { it.updateId.toString() == cprUsageId }
  }

  companion object {

    fun Pair<PrisonAddressUsage, AddressUsageEntity>.toMapping() = SysconAddressUsageMapping(
      nomisAddressUsageId = first.nomisAddressUsageId,
      nomisAddressUsageCode = first.addressUsageCode,
      cprAddressUsageId = second.updateId.toString(),
    )

    fun AddressUsageEntity.updateFrom(prisonAddressUsage: PrisonAddressUsage) {
      usageCode = prisonAddressUsage.addressUsageCode
      active = prisonAddressUsage.isActive
      modifyDateTime = prisonAddressUsage.modifyDateTime
      modifyUserId = prisonAddressUsage.modifyUserId
      createDateTime = prisonAddressUsage.createDateTime
      createUserId = prisonAddressUsage.createUserId
    }

    fun PrisonAddressUsage.toEntity(addressEntity: AddressEntity) = AddressUsageEntity(
      usageCode = addressUsageCode,
      active = isActive,
      address = addressEntity,
      modifyDateTime = modifyDateTime,
      modifyUserId = modifyUserId,
      createDateTime = createDateTime,
      createUserId = createUserId,
    )
  }
}
