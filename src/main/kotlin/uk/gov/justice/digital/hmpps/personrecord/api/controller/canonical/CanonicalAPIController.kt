package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import java.util.UUID

@Tag(name = "Canonical")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
class CanonicalAPIController(
  private val personKeyRepository: PersonKeyRepository,
) {
  @Operation(
    description = "**Note: This API endpoint is scheduled for deprecation.**\n\n " +
      "Retrieve person record by UUID. Role required is **$API_READ_ONLY**",
    security = [io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "api-role")],
    deprecated = true,
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
  )
  fun getCanonicalRecord(
    @PathVariable(name = "uuid") uuid: UUID,
  ): ResponseEntity<*> {
    val personKeyEntity = personKeyRepository.findByPersonUUID(uuid)
    return when {
      personKeyEntity == null -> throw ResourceNotFoundException(uuid.toString())
      else -> ResponseEntity.ok(CanonicalRecord.from(personKeyEntity))
    }
  }
}
