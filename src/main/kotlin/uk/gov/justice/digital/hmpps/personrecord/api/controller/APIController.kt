package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CanonicalRecordNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import java.util.UUID

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('${Roles.API_READ_ONLY}')")
class APIController(
  private val personKeyRepository: PersonKeyRepository,
) {
  @Operation(
    description = "Retrieve person record by UUID. Role required is **${Roles.API_READ_ONLY}**",
    security = [io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/person/{uuid}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
  )
  fun getCanonicalRecord(
    @PathVariable(name = "uuid") uuid: UUID,
  ): CanonicalRecord {
    val personKeyEntity = getCorrectPersonKeyEntity(personKeyRepository.findByPersonId(uuid))
    return buildCanonicalRecord(personKeyEntity, uuid)
  }

  private fun getCorrectPersonKeyEntity(personKeyEntity: PersonKeyEntity?, existingMergeChain: MutableSet<UUID?>? = mutableSetOf()): PersonKeyEntity? {
    if (existingMergeChain?.contains(personKeyEntity?.personId) == true) {
      throw IllegalStateException("Circular merge reference")
    }
    existingMergeChain?.add(personKeyEntity?.personId)
    return personKeyEntity?.mergedTo?.let { getCorrectPersonKeyEntity(personKeyRepository.findByIdOrNull(it), existingMergeChain) }
      ?: personKeyEntity
  }

  private fun buildCanonicalRecord(personKeyEntity: PersonKeyEntity?, uuid: UUID): CanonicalRecord = when {
    personKeyEntity?.personEntities?.isNotEmpty() == true -> CanonicalRecord.from(personKeyEntity)
    else -> throw CanonicalRecordNotFoundException(uuid)
  }
}
