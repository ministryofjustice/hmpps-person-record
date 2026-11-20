package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncPrimaryNameHandler

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class SysconSyncPrisonPrimaryNameAPIController(
  private val sysconSyncPrimaryNameHandler: SysconSyncPrimaryNameHandler,
) {
  @Operation(
    description = """Sets the given alias as the PRIMARY (working) name for the prisoner, marking all other aliases as ALIAS. Does nothing if the alias is already the PRIMARY name. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/syscon-sync/person/{prisonNumber}/primary-name/{cprAliasId}")
  fun processPrisonPrimaryName(
    @PathVariable prisonNumber: String,
    @PathVariable cprAliasId: String,
  ) {
    sysconSyncPrimaryNameHandler.handleSetPrimaryName(prisonNumber, cprAliasId)
  }
}
