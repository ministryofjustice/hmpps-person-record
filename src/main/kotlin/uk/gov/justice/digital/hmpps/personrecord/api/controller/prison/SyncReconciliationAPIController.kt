package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SyncReconciliationGetHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.SyncReconciliation

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('$PERSON_RECORD_SYSCON_SYNC_WRITE')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SyncReconciliationAPIController(
  private val syncReconciliationGetHandler: SyncReconciliationGetHandler,
) {

  @Operation(
    description = "Retrieve the prison data synchronised from NOMIS for a person, by Prison Number, for reconciliation. " +
      "Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/syscon-sync/person/{prisonNumber}/reconciliation")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
  )
  fun getReconciliation(@PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String): ResponseEntity<SyncReconciliation> = syncReconciliationGetHandler.get(prisonNumber)
}
