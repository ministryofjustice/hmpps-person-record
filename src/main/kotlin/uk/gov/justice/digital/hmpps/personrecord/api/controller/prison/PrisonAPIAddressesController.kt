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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonAddressesHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonContactsHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAddressRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactResponse

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
  fun getAddressesByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
  ): List<CanonicalAddress> = prisonAddressesHandler.get(prisonNumber)

  @Operation(
    description = "Add an address for the prisoner by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  fun createAddressByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @Valid @RequestBody request: PrisonAddressRequest,
  ): ResponseEntity<CanonicalAddress> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Add phone numbers for a prisoner's address by Prison Number and address id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/addresses/{addressId}/phone-numbers")
  @ResponseStatus(HttpStatus.CREATED)
  fun createAddressPhoneNumbersByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @PathVariable(name = "addressId") @Parameter(description = "The address id") addressId: String,
    @Valid @RequestBody request: List<@Valid PrisonContactRequest>,
  ): List<PrisonContactResponse> = prisonContactsHandler.createPhoneNumbers(addressId, request)
}
