package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import java.util.UUID

@Component
class PrisonReligionUpdateHandler(private val prisonReligionRepository: PrisonReligionRepository) {

  @Transactional
  fun handleUpdate(cprReligionId: String, updateRequest: PrisonReligionUpdateRequest) {
    val existingPrisonReligion = validateRequest(cprReligionId)
    updatePrisonReligion(updateRequest, existingPrisonReligion)
  }

  fun validateRequest(cprReligionId: String): PrisonReligionEntity {
    val existingPrisonReligion = prisonReligionRepository.findByUpdateId(UUID.fromString(cprReligionId))
      ?: throw ResourceNotFoundException("Prison religion with $cprReligionId not found")

    return existingPrisonReligion
  }

  fun updatePrisonReligion(updateRequest: PrisonReligionUpdateRequest, existingPrisonReligion: PrisonReligionEntity): PrisonReligionEntity {
    existingPrisonReligion.comments = updateRequest.comments
    existingPrisonReligion.modifyDateTime = updateRequest.modifyDateTime
    existingPrisonReligion.modifyUserId = updateRequest.modifyUserId
    return prisonReligionRepository.save(existingPrisonReligion)
  }
}
