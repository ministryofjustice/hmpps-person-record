package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconAliasesAndIdentifiersMigrationHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonAliasesIdentifiersAPIController(
  private val migrationHandler: SysconAliasesAndIdentifiersMigrationHandler?,
) {

  @Operation(description = "Save the prison aliases and identifers for the given prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/syscon-sync/aliases-identifiers/{prisonNumber}")
  fun saveAliasesAndIdentifiers(
    @PathVariable prisonNumber: String,
    @Valid @RequestBody aliasAndIdentifiersRequest: PrisonAliasesAndIdentifiersRequest,
  ): ResponseEntity<SysconAliasesAndIdentifiersResponseBody> {
    val handler = migrationHandler ?: return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    val response = handler.handleInsert(prisonNumber, aliasAndIdentifiersRequest)
    return ResponseEntity.status(HttpStatus.CREATED).body(response)
  }
}
