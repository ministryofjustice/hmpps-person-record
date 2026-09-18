package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import io.swagger.v3.oas.annotations.Hidden
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
import uk.gov.justice.digital.hmpps.personrecord.api.handler.search.VettingPersonSearchHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse

@Tag(name = "Vetting")
@RestController
class VettingPersonSearchController(
  private val vettingPersonSearchHandler: VettingPersonSearchHandler,
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = """
        This endpoint returns person matches grouped by their associated clusters.
      """,
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = VettingPersonSearchResponse::class),
        ),
      ],
    ),
  )
  @Hidden
  @PreAuthorize("hasRole('$API_VETTING_SEARCH_ONLY')")
  @PostMapping("/person/vetting/search")
  fun vettingSearch(
    @RequestBody personSearchRequest: VettingPersonSearchRequest,
  ): ResponseEntity<VettingPersonSearchResponse> {
    val result = vettingPersonSearchHandler.search(personSearchRequest)
    return ResponseEntity(result, HttpStatus.OK)
  }
}
