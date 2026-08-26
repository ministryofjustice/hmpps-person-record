package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class SysconSyncPrisonMergeAPIController(
  private val prisonMergeEventProcessor: PrisonMergeEventProcessor,
) {
  @Operation(
    description = """Processes a prisoner merge record by Prison Number. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ResponseStatus(NO_CONTENT)
  @PostMapping("/syscon-sync/person/{prisonNumber}/merge")
  fun processPrisonMerge(
    @PathVariable prisonNumber: String,
    @RequestBody prisonMergeRequest: PrisonMerge,
  ) {
    prisonMergeEventProcessor.processEvent(
      fromPrisonNumber = prisonMergeRequest.fromPrisonNumber,
      toPrisonNumber = prisonNumber,
    )
  }
}
