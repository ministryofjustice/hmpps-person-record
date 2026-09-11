package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_VETTING_SEARCH_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.handler.search.PersonSearchHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse

@Tag(name = "Vetting")
@RestController
class VettingPersonSearchController(
  private val personSearchHandler: PersonSearchHandler,
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = """
        This endpoint returns matching person records from Prison and Probation only.
        The objects in the top level array will be ordered by the strongest match descending (first record strongest, last record weakest).
      """,
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = VettingPersonSearchResponse::class),
        ),
      ],
    ),
  )
  @PreAuthorize("hasRole('$API_VETTING_SEARCH_ONLY')")
  @PostMapping("/person/vetting/search")
  fun personSearch(
    @RequestBody personSearchRequest: VettingPersonSearchRequest,
  ): ResponseEntity<VettingPersonSearchResponse> {
    val result = personSearchHandler.search(personSearchRequest)
    return ResponseEntity(result, HttpStatus.OK)
  }
}
