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
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifierCreateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifierResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifierUpdateRequest

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIIdentifiersController {

  @Operation(
    description = "Retrieve all identifiers for the prisoner by Prison Number, optionally including alias identifiers. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/identifiers")
  fun getIdentifiersByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @RequestParam(name = "includeAliases", required = false, defaultValue = "false")
    @Parameter(description = "Whether to include identifiers on aliases")
    includeAliases: Boolean,
  ): ResponseEntity<List<PrisonIdentifierResponse>> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Retrieve a single identifier for the prisoner's alias by Prison Number, alias id and identifier id. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/aliases/{cprAliasId}/identifiers/{cprIdentifierId}")
  fun getAliasIdentifierByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "cprAliasId") @Parameter(description = "The CPR alias id") cprAliasId: String,
    @PathVariable(name = "cprIdentifierId") @Parameter(description = "The CPR identifier id") cprIdentifierId: String,
  ): ResponseEntity<PrisonIdentifierResponse> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Update an existing identifier for the prisoner's alias by Prison Number, alias id and identifier id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/aliases/{cprAliasId}/identifiers/{cprIdentifierId}")
  fun updateAliasIdentifierByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "cprAliasId") @Parameter(description = "The CPR alias id") cprAliasId: String,
    @PathVariable(name = "cprIdentifierId") @Parameter(description = "The CPR identifier id") cprIdentifierId: String,
    @Valid @RequestBody request: PrisonIdentifierUpdateRequest,
  ): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Add identifiers for the prisoner on the primary name by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/identifiers")
  fun createIdentifiersByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @Valid @RequestBody request: List<@Valid PrisonIdentifierCreateRequest>,
  ): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
