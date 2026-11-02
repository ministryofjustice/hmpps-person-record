package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonAddressesHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonContactsHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCreateAddressRequest

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIAddressesController(
  private val prisonAddressesHandler: PrisonAddressesHandler,
  private val prisonContactsHandler: PrisonContactsHandler,
) {
  @Operation(
    description = "Retrieve the prisoner's addresses by Prison Number, most recent first. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/addresses")
  fun getAddressesByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
  ): List<CanonicalAddress> = prisonAddressesHandler.get(prisonNumber)

  @Operation(
    description = "Add an address for the prisoner by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  fun createAddressByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @Valid @RequestBody request: PrisonCreateAddressRequest,
  ): CanonicalAddress = prisonAddressesHandler.create(prisonNumber, request)

  @Operation(
    description = "Add contacts to a prisoner's address by Prison Number and address id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/addresses/{addressId}/contacts")
  @ResponseStatus(HttpStatus.CREATED)
  fun createAddressContactsByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "addressId") @Parameter(description = "The address id") addressId: String,
    @Valid @RequestBody request: List<@Valid PrisonContactRequest>,
  ): List<CanonicalContact> = prisonContactsHandler.createContacts(prisonNumber, addressId, request)

  @Operation(
    description = "Update contacts for a prisoner's address by Prison Number and address id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/addresses/{addressId}/contacts/{contactId}")
  fun updateAddressContactsByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "addressId") @Parameter(description = "The address id") addressId: String,
    @PathVariable(name = "contactId") @Parameter(description = "The contact id") contactId: String,
    @Valid @RequestBody request: PrisonContactRequest,
  ): CanonicalContact = prisonContactsHandler.updateContact(prisonNumber, addressId, contactId, request)
}
