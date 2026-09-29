package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SysconSyncPrisonContactsAPIController {

  @Operation(
    description = """Create prisoner contact record by Prison Number. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PostMapping("/syscon-sync/person/{prisonNumber}/contact")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPrisonerContact(
    @PathVariable prisonNumber: String,
    @RequestBody requestBody: PrisonContact,
  ): ResponseEntity<SysconContactMapping> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

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
  ): ResponseEntity<SysconContactMapping> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = """Update prisoner contact record by Prison Number and contact uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PutMapping("/syscon-sync/person/{prisonNumber}/contact/{cprContactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updatePrisonerContact(
    @PathVariable prisonNumber: String,
    @PathVariable cprContactId: String,
    @RequestBody requestBody: PrisonContact,
  ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = """Delete prisoner contact record by Prison Number and contact uuid. Role required is **${PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @DeleteMapping("/syscon-sync/person/{prisonNumber}/contact/{cprContactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deletePrisonerContact(
    @PathVariable prisonNumber: String,
    @PathVariable cprContactId: String,
  ): ResponseEntity<Unit> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
