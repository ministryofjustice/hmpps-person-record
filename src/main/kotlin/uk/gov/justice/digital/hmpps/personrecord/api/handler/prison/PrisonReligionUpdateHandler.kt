package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import java.util.UUID

@Component
class PrisonReligionUpdateHandler(private val prisonReligionRepository: PrisonReligionRepository) {

  @Transactional
  fun handleUpdate(cprReligionId: String, updateRequest: PrisonReligionUpdateRequest): PrisonReligionMapping {
    val existingPrisonReligion = validateRequest(cprReligionId, updateRequest)
    val updatedPrisonReligion = updatePrisonReligion(updateRequest, existingPrisonReligion)
    return PrisonReligionMapping(
      nomisReligionId = updateRequest.nomisReligionId,
      cprReligionId = updatedPrisonReligion.updateId.toString(),
    )
  }

  fun validateRequest(cprReligionId: String, updateRequest: PrisonReligionUpdateRequest): PrisonReligionEntity {
    val existingPrisonReligion = prisonReligionRepository.findByUpdateId(UUID.fromString(cprReligionId))
      ?: throw ResourceNotFoundException("Prison religion with $cprReligionId not found")

    if (isAttemptingToPromoteHistoricPrisonReligion(existingPrisonReligion, updateRequest)) {
      throw IllegalArgumentException("Prison religion update request with update id '$cprReligionId' can not change a non current religion to current")
    }
    return existingPrisonReligion
  }

  private fun isAttemptingToPromoteHistoricPrisonReligion(
    existingPrisonReligion: PrisonReligionEntity,
    updateRequest: PrisonReligionUpdateRequest,
  ) = updateRequest.current && existingPrisonReligion.prisonRecordType == PrisonRecordType.HISTORIC

  fun updatePrisonReligion(updateRequest: PrisonReligionUpdateRequest, existingPrisonReligion: PrisonReligionEntity): PrisonReligionEntity {
    existingPrisonReligion.comments = updateRequest.comments
    existingPrisonReligion.verified = updateRequest.verified
    existingPrisonReligion.modifyDateTime = updateRequest.modifyDateTime
    existingPrisonReligion.modifyUserId = updateRequest.modifyUserId
    existingPrisonReligion.endDate = updateRequest.endDate
    existingPrisonReligion.prisonRecordType = PrisonRecordType.from(updateRequest.current)
    return prisonReligionRepository.save(existingPrisonReligion)
  }
}
