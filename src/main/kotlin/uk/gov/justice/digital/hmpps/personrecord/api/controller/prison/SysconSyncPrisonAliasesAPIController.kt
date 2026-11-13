package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasWithIdentifiersMapping

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonAliasesAPIController {

  @Operation(
    description = """Get a prisoner alias record by Prison Number and alias uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/syscon-sync/person/{prisonNumber}/alias/{cprAliasId}")
  fun getPrisonerAlias(
    @PathVariable prisonNumber: String,
    @PathVariable cprAliasId: String,
  ): ResponseEntity<PrisonAlias> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = """Create prisoner alias record, including its identifiers, by Prison Number. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PostMapping("/syscon-sync/person/{prisonNumber}/alias")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPrisonerAlias(
    @PathVariable prisonNumber: String,
    @Valid @RequestBody requestBody: PrisonAlias,
  ): ResponseEntity<SysconAliasWithIdentifiersMapping> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = """Update prisoner alias record by Prison Number and alias uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PutMapping("/syscon-sync/person/{prisonNumber}/alias/{cprAliasId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updatePrisonerAlias(
    @PathVariable prisonNumber: String,
    @PathVariable cprAliasId: String,
    @Valid @RequestBody requestBody: PrisonAlias,
  ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = """Delete prisoner alias record by Prison Number and alias uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @DeleteMapping("/syscon-sync/person/{prisonNumber}/alias/{cprAliasId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deletePrisonerAlias(
    @PathVariable prisonNumber: String,
    @PathVariable cprAliasId: String,
  ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
