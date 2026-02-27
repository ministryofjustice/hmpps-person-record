package uk.gov.justice.digital.hmpps.personrecord.api.handler

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class PrisonReligionUpdateHandler(private val prisonReligionRepository: PrisonReligionRepository) {

  @Transactional
  fun handleUpdate(request: PrisonReligionPatchRequest): PrisonReligionMapping {
    val existingPrisonReligion = prisonReligionRepository.findByUpdateId(UUID.fromString(request.cprReligionId))
      ?: throw ResourceNotFoundException("Prison religion with ${request.cprReligionId} not found")

    existingPrisonReligion.comments = request.comments
    existingPrisonReligion.verified = request.verified
    existingPrisonReligion.modifyDateTime = request.modifyDateTime
    existingPrisonReligion.modifyUserId = request.modifyUserId
    existingPrisonReligion.endDate = request.endDate
    existingPrisonReligion.prisonRecordType = PrisonRecordType.from(request.current)

    val updatedPrisonReligion = prisonReligionRepository.save(existingPrisonReligion)
    return PrisonReligionMapping(
      nomisReligionId = request.nomisReligionId,
      cprReligionId = updatedPrisonReligion.updateId.toString(),
    )
  }

  data class PrisonReligionPatchRequest(
    val prisonNumber: String,
    val cprReligionId: String,
    val nomisReligionId: String,
    val current: Boolean,
    val endDate: LocalDate? = null,
    val comments: String? = null,
    val verified: Boolean? = null,
    val modifyDateTime: LocalDateTime,
    val modifyUserId: String,
  ) {

    companion object {
      fun from(prisonNumber: String, cprReligionId: String, prisonReligion: PrisonReligion) = PrisonReligionPatchRequest(
        prisonNumber = prisonNumber,
        cprReligionId = cprReligionId,
        nomisReligionId = prisonReligion.nomisReligionId,
        current = prisonReligion.current,
        endDate = prisonReligion.endDate,
        comments = prisonReligion.comments,
        verified = prisonReligion.verified,
        modifyDateTime = prisonReligion.modifyDateTime,
        modifyUserId = prisonReligion.modifyUserId,
      )
    }
  }
}
