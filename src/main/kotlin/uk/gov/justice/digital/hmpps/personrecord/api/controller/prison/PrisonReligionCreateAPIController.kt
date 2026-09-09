package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionInsertRequest

@Tag(name = "Prison")
@RestController
class PrisonReligionCreateAPIController(
  private val prisonReligionInsertHandler: PrisonReligionInsertHandler,
) {
  @Operation(
    description = """Create a religion for the given prison number. Role required is **$PRISON_API_READ_WRITE**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Religion created in CPR",
    ),
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/person/prison/{prisonNumber}/religion")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPrisonReligion(
    @PathVariable prisonNumber: String,
    @RequestBody prisonReligionInsertRequest: PrisonReligionInsertRequest,
  ) {
    prisonReligionInsertHandler.handleInsert(prisonNumber, prisonReligionInsertRequest)
  }
}
