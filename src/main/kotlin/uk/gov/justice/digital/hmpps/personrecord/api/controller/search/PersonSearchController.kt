package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_SEARCH_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.handler.search.PersonSearchHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchResponse

@Tag(name = "Search")
@RestController
@Profile("!preprod & !prod")
class PersonSearchController(
  private val personSearchHandler: PersonSearchHandler,
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "The root objects in the array will be ordered by the strongest match descending.",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PersonSearchResponse::class),
        ),
      ],
    ),
  )
  @PreAuthorize("hasRole('$API_SEARCH_ONLY')")
  @PostMapping("/person/search")
  fun personSearch(
    @RequestBody personSearchRequest: PersonSearchRequest,
  ): ResponseEntity<PersonSearchResponse> {
    val result = personSearchHandler.search(personSearchRequest)
    return ResponseEntity(result, HttpStatus.OK)
  }
}
