package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CanonicalRecordNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import java.net.URI
import java.util.UUID

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
class APIController(
  private val personKeyRepository: PersonKeyRepository,
) {
  @Operation(
    description = "Retrieve person record by UUID. Role required is **$API_READ_ONLY**",
    security = [io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/person/{uuid}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CanonicalRecord::class),
        ),
      ],
    ),
    ApiResponse(
      responseCode = "301",
      description = "Permanent Redirect",
      content = [
        Content(schema = Schema(hidden = true)),
      ],
    ),
  )
  fun getCanonicalRecord(
    @PathVariable(name = "uuid") uuid: UUID,
  ): ResponseEntity<*> {
    val personKeyEntity = getCorrectPersonKeyEntity(personKeyRepository.findByPersonUUID(uuid), mutableSetOf())
    return when {
      personKeyEntity == null -> throw CanonicalRecordNotFoundException(uuid)
      personKeyEntity.isNotRequestedUuid(uuid) -> ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI("/person/${personKeyEntity.personUUID}")).build<Void>()
      else -> ResponseEntity.ok(CanonicalRecord.from(personKeyEntity))
    }
  }

  private fun getCorrectPersonKeyEntity(personKeyEntity: PersonKeyEntity?, existingMergeChain: MutableSet<UUID?>): PersonKeyEntity? = personKeyEntity?.mergedTo?.let {
    existingMergeChain.add(personKeyEntity.personUUID)
    getCorrectPersonKeyEntity(personKeyRepository.findByIdOrNull(it), existingMergeChain)
  }
    ?: personKeyEntity

  private fun PersonKeyEntity.isNotRequestedUuid(uuid: UUID): Boolean = this.personUUID != uuid
}
