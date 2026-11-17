package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReference
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReferenceCreateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReferenceUpdateRequest

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIReferencesController {

  @Operation(
    description = "Retrieve all references for the prisoner by Prison Number, optionally including pseudonym references. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/references")
  fun getReferencesByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @RequestParam(name = "includePseudonyms", required = false, defaultValue = "false")
    @Parameter(description = "Whether to include references on pseudonyms")
    includePseudonyms: Boolean,
  ): ResponseEntity<List<PrisonReference>> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Retrieve a single reference for the prisoner's pseudonym by Prison Number, pseudonym id and reference id. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/pseudonyms/{cprPseudonymId}/references/{cprReferenceId}")
  fun getPseudonymReferenceByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "cprPseudonymId") @Parameter(description = "The CPR pseudonym id") cprPseudonymId: String,
    @PathVariable(name = "cprReferenceId") @Parameter(description = "The CPR reference id") cprReferenceId: String,
  ): ResponseEntity<PrisonReference> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Update an existing reference for the prisoner's pseudonym by Prison Number, pseudonym id and reference id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/pseudonyms/{cprPseudonymId}/references/{cprReferenceId}")
  fun updatePseudonymReferenceByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "cprPseudonymId") @Parameter(description = "The CPR pseudonym id") cprPseudonymId: String,
    @PathVariable(name = "cprReferenceId") @Parameter(description = "The CPR reference id") cprReferenceId: String,
    @Valid @RequestBody request: PrisonReferenceUpdateRequest,
  ): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Add references for the prisoner on the primary name by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/references")
  fun createReferencesByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @Valid @RequestBody request: List<@Valid PrisonReferenceCreateRequest>,
  ): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
