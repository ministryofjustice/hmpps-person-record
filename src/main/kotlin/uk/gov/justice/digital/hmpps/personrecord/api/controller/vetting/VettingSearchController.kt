package uk.gov.justice.digital.hmpps.personrecord.api.controller.vetting

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.handler.vetting.VettingSearchHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse

@Tag(name = "Vetting Search")
@RestController
@Profile("dev")
class VettingSearchController(
  private val vettingSearchHandler: VettingSearchHandler,
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = VettingSearchResponse::class),
        ),
      ],
    ),
  )
  @PreAuthorize("hasRole('$API_READ_ONLY')")
  @GetMapping
  fun vettingSearch(
    @RequestBody vettingSearchRequest: VettingSearchRequest,
  ): ResponseEntity<VettingSearchResponse> {
    val result = vettingSearchHandler.search(vettingSearchRequest)
    return ResponseEntity(result, HttpStatus.OK)
  }
}
