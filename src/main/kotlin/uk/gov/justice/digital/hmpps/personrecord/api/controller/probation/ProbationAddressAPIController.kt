package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import java.util.UUID

@Tag(name = "Probation")
@RestController
class ProbationAddressAPIController(
  private val addressRepository: AddressRepository,
) {

  @Operation(
    description = """Retrieve an address record by CPR Address Id. Role required is **$API_READ_ONLY**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CanonicalAddress::class),
        ),
      ],
    ),
  )
  @PreAuthorize("hasRole('$API_READ_ONLY')")
  @GetMapping("/person/probation/{crn}/address/{cprAddressId}")
  fun getProbationAddress(
    @PathVariable(name = "crn") crn: String,
    @PathVariable(name = "cprAddressId") cprAddressId: String,
  ): CanonicalAddress {
    val address = addressRepository.findByUpdateIdAndPersonCrn(UUID.fromString(cprAddressId), crn)
      ?: throw ResourceNotFoundException(cprAddressId)

    return CanonicalAddress.from(address)
  }
}
