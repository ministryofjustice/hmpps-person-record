package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactResponse
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIContactsController {

  @Operation(
    description = "Retrieve the prisoner's contacts by Prison Number, optionally filtered by contact type. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/contacts")
  fun getContactsByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @RequestParam(name = "includeTypes", required = false)
    @Parameter(description = "Only return contacts of these types", example = "HOME,MOBILE")
    includeTypes: List<ContactType>? = null,
    @RequestParam(name = "excludeTypes", required = false)
    @Parameter(description = "Do not return contacts of these types", example = "EMAIL")
    excludeTypes: List<ContactType>? = null,
  ): ResponseEntity<List<PrisonContactResponse>> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Add a contact for the prisoner by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/contacts")
  @ResponseStatus(HttpStatus.CREATED)
  fun createContactByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @Valid @RequestBody request: PrisonContactRequest,
  ): ResponseEntity<PrisonContactResponse> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Update a contact for the prisoner by Prison Number and contact id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/contacts/{contactId}")
  fun updateContactByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @PathVariable(name = "contactId") @Parameter(description = "The contact id") contactId: String,
    @Valid @RequestBody request: PrisonContactRequest,
  ): ResponseEntity<PrisonContactResponse> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
