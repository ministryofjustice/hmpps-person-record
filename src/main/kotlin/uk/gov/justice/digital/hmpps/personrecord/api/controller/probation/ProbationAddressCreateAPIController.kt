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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.probation.ProbationAddressCreateHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.probation.ProbationCreateAddressResponse
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address

@Tag(name = "Probation")
@RestController
class ProbationAddressCreateAPIController(
  private val probationAddressCreateHandler: ProbationAddressCreateHandler,
) {
  @Operation(
    description = """Create an address for the given CRN person record. Role required is **$PROBATION_API_READ_WRITE**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Address created in CPR",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ProbationCreateAddressResponse::class),
        ),
      ],
    ),
  )
  @PreAuthorize("hasRole('$PROBATION_API_READ_WRITE')")
  @PostMapping("/person/probation/{crn}/address")
  fun createProbationAddress(
    @PathVariable crn: String,
    @RequestBody probationCreateAddress: ProbationCreateAddress,
  ): ResponseEntity<ProbationCreateAddressResponse> {
    val createdAddress = probationAddressCreateHandler.create(crn, Address.from(probationCreateAddress))
    val responseBody = ProbationCreateAddressResponse(crn, createdAddress.updateId!!.toString())
    return ResponseEntity.status(HttpStatus.CREATED).body(responseBody)
  }
}
