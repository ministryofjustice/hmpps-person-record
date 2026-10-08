package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncAddressUsagesHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage

@Profile("!prod && !preprod")
@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonAddressUsagesAPIController(
  private val sysconSyncAddressUsagesHandler: SysconSyncAddressUsagesHandler,
) {

  @Operation(
    description = """Create prisoner address usage record by Prison Number and address uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PostMapping("/syscon-sync/person/{prisonNumber}/address/{cprAddressId}/usage")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPrisonerAddressUsage(
    @PathVariable prisonNumber: String,
    @PathVariable cprAddressId: String,
    @RequestBody requestBody: PrisonAddressUsage,
  ) = sysconSyncAddressUsagesHandler.handleInsert(prisonNumber, cprAddressId, requestBody)

  @Operation(
    description = """Update prisoner address usage record by Prison Number, address uuid and usage uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PutMapping("/syscon-sync/person/{prisonNumber}/address/{cprAddressId}/usage/{cprUsageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updatePrisonerAddressUsage(
    @PathVariable prisonNumber: String,
    @PathVariable cprAddressId: String,
    @PathVariable cprUsageId: String,
    @RequestBody requestBody: PrisonAddressUsage,
  ) = sysconSyncAddressUsagesHandler.handleUpdate(prisonNumber, cprAddressId, cprUsageId, requestBody)

  @Operation(
    description = """Delete prisoner address usage record by Prison Number, address uuid and usage uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @DeleteMapping("/syscon-sync/person/{prisonNumber}/address/{cprAddressId}/usage/{cprUsageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deletePrisonerAddressUsage(
    @PathVariable prisonNumber: String,
    @PathVariable cprAddressId: String,
    @PathVariable cprUsageId: String,
  ) = sysconSyncAddressUsagesHandler.handleDelete(prisonNumber, cprAddressId, cprUsageId)
}
