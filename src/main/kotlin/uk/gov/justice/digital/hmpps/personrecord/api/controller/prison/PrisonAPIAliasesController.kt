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
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAliasCreateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAliasResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAliasUpdateRequest

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIAliasesController {

  @Operation(
    description = "Retrieve all aliases (including the primary name) for the prisoner by Prison Number. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/aliases")
  fun getAliasesByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
  ): ResponseEntity<List<PrisonAliasResponse>> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Retrieve a single alias for the prisoner by Prison Number and alias id. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/aliases/{cprAliasId}")
  fun getAliasByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "cprAliasId") @Parameter(description = "The CPR alias id") cprAliasId: String,
  ): ResponseEntity<PrisonAliasResponse> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Create a new alias for the prisoner by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/aliases")
  fun createAliasByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @Valid @RequestBody request: PrisonAliasCreateRequest,
  ): ResponseEntity<PrisonAliasResponse> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Update an existing alias for the prisoner by Prison Number and alias id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/aliases/{cprAliasId}")
  fun updateAliasByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "cprAliasId") @Parameter(description = "The CPR alias id") cprAliasId: String,
    @Valid @RequestBody request: PrisonAliasUpdateRequest,
  ): ResponseEntity<PrisonAliasResponse> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
