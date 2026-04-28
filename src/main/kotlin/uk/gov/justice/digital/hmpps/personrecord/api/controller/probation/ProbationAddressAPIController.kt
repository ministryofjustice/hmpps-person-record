package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationAddressCreateResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.Address as ProbationAddress

@Tag(name = "Probation")
@RestController
class ProbationAddressAPIController(
  private val addressService: AddressService,
  private val addressRepository: AddressRepository,
  private val personRepository: PersonRepository,
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

  @Operation(
    description = """Create an address for the given CRN person record. Role required is **$PROBATION_API_READ_WRITE**.""",
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
  @PreAuthorize("hasRole('$PROBATION_API_READ_WRITE')")
  @PostMapping("/person/probation/{crn}/address")
  @Transactional(isolation = REPEATABLE_READ)
  fun createProbationAddress(
    @PathVariable(name = "crn") crn: String,
    @RequestBody probationAddress: ProbationAddress,
  ): ResponseEntity<ProbationAddressCreateResponse> {
    val address = Address.from(probationAddress)

    val createdAddress: AddressEntity = addressService.processAddress(
      address,
      findPerson = { personRepository.findByCrn(crn) },
      findAddress = { null },
    )

    val responseBody = ProbationAddressCreateResponse(createdAddress.updateId!!.toString())
    return ResponseEntity.status(HttpStatus.CREATED).body(responseBody)
  }
}
