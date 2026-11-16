package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class SysconSyncPrisonPrimaryNameAPIController {
  @Operation(
    description = """Sets the given pseudonym as the PRIMARY (working) name for the prisoner, marking all other pseudonyms as ALIAS. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/syscon-sync/person/{prisonNumber}/primary-name/{cprPseudonymId}")
  fun processPrisonPrimaryName(
    @PathVariable prisonNumber: String,
    @PathVariable cprPseudonymId: String,
  ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
