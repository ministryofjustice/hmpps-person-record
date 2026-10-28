package uk.gov.justice.digital.hmpps.personrecord.api.controller.refdata

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.refdata.ReferenceDataValue
import uk.gov.justice.digital.hmpps.personrecord.service.refdata.ReferenceDataService

@Tag(name = "Reference Data")
@RestController
@RequestMapping("/person/reference-data")
@PreAuthorize("hasRole('$API_READ_ONLY')")
class ReferenceDataAPIController(
  private val referenceDataService: ReferenceDataService,
) {

  @Operation(
    description = """Retrieve all reference data, grouped by reference data type. Role required is **$API_READ_ONLY**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
  )
  @GetMapping("/type/alltypes")
  fun getAllReferenceData(): Map<String, List<ReferenceDataValue>> = referenceDataService.getAllReferenceData()

  @Operation(
    description = """Retrieve reference data for a single reference data type, e.g. sex, religion, nationality, grouped by its canonical type key.
      |Role required is **$API_READ_ONLY**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(responseCode = "404", description = "Unknown reference data type"),
  )
  @GetMapping("/type/{type}")
  fun getReferenceData(
    @PathVariable type: String,
  ): Map<String, List<ReferenceDataValue>> = referenceDataService.getReferenceData(type)
}
