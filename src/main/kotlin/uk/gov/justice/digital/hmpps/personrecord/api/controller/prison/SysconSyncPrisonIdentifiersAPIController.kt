package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifiersResponseBody

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonIdentifiersAPIController {

  @Operation(description = "Replace the prison identifiers for the given prison number and alias. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @ResponseStatus(HttpStatus.OK)
  @PutMapping("/syscon-sync/person/{prisonNumber}/alias/{cprAliasId}/identifiers")
  fun replaceIdentifiers(
    @PathVariable prisonNumber: String,
    @PathVariable cprAliasId: String,
    @Valid @RequestBody identifiersRequest: PrisonIdentifiersRequest,
  ): ResponseEntity<SysconIdentifiersResponseBody> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
