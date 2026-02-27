package uk.gov.justice.digital.hmpps.personrecord.api.handler

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequestBody
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import java.util.UUID

@Component
class PrisonReligionUpdateHandler(private val prisonReligionRepository: PrisonReligionRepository) {

  @Transactional
  fun handleUpdate(cprReligionId: String, updateRequest: PrisonReligionUpdateRequestBody): PrisonReligionMapping {
    val existingPrisonReligion = prisonReligionRepository.findByUpdateId(UUID.fromString(cprReligionId))
      ?: throw ResourceNotFoundException("Prison religion with $cprReligionId not found")

    existingPrisonReligion.comments = updateRequest.comments
    existingPrisonReligion.verified = updateRequest.verified
    existingPrisonReligion.modifyDateTime = updateRequest.modifyDateTime
    existingPrisonReligion.modifyUserId = updateRequest.modifyUserId
    existingPrisonReligion.endDate = updateRequest.endDate
    existingPrisonReligion.prisonRecordType = PrisonRecordType.from(updateRequest.current)

    val updatedPrisonReligion = prisonReligionRepository.save(existingPrisonReligion)
    return PrisonReligionMapping(
      nomisReligionId = updateRequest.nomisReligionId,
      cprReligionId = updatedPrisonReligion.updateId.toString(),
    )
  }
}
