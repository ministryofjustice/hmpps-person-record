package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonMergeAPIController {
  @Operation(
    description = """Processes a prisoner merge record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/syscon-sync/person/{prisonNumber}/merge")
  fun processPrisonMerge(
    @PathVariable prisonNumber: String,
    @RequestBody prisonMergeRequest: PrisonMerge,
  ) {
    log.info("Ignoring prison merge for prison number: {} from prison number: {}", prisonNumber, prisonMergeRequest.fromPrisonNumber)
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
