package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconContactsAndAddressesMigrationHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncContactsHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact

@Profile("!prod && !preprod")
@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonAddressesContactsMigrationAPIController(
  private val sysconAliasesAndIdentifiersMigrationHandler: SysconContactsAndAddressesMigrationHandler,
  private val sysconSyncContactsHandler: SysconSyncContactsHandler,
) {
  @Operation(
    description = "Save the prisoner addresses and contacts for the given prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/syscon-sync/addresses-contacts/{prisonNumber}")
  fun saveAddressesAndContacts(
    @PathVariable prisonNumber: String,
    @Valid @RequestBody addressesAndContactsRequest: PrisonAddressesAndContactsRequest,
  ) = sysconAliasesAndIdentifiersMigrationHandler.handleInsert(prisonNumber, addressesAndContactsRequest)

  @Operation(
    description = """Create prisoner address contact record by Prison Number and address uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PostMapping("/syscon-sync/person/{prisonNumber}/address/{cprAddressId}/contact")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPrisonerAddressContact(
    @PathVariable prisonNumber: String,
    @PathVariable cprAddressId: String,
    @RequestBody requestBody: PrisonContact,
  ) = sysconSyncContactsHandler.handleInsert(prisonNumber, cprAddressId, requestBody)

  @Operation(
    description = """Update prisoner address contact record by Prison Number, address uuid and contact uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PutMapping("/syscon-sync/person/{prisonNumber}/address/{cprAddressId}/contact/{cprContactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updatePrisonerAddressContact(
    @PathVariable prisonNumber: String,
    @PathVariable cprAddressId: String,
    @PathVariable cprContactId: String,
    @RequestBody requestBody: PrisonContact,
  ) = sysconSyncContactsHandler.handleUpdate(prisonNumber, cprContactId, requestBody)

  @Operation(
    description = """Delete prisoner address contact record by Prison Number and contact uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @DeleteMapping("/syscon-sync/person/{prisonNumber}/address/contact/{cprContactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deletePrisonerAddressContact(
    @PathVariable prisonNumber: String,
    @PathVariable cprContactId: String,
  ) = sysconSyncContactsHandler.handleDelete(prisonNumber, cprContactId)
}
