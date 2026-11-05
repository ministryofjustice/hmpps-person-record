package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.ExactMatchSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.ExactMatchSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Tag(name = "Search")
@RestController
class ExactMatchSearchController(
  private val personMatchClient: PersonMatchClient,
  private val personRepository: PersonRepository,
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "This endpoint returns person identifiers which exactly match all the search terms",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ExactMatchSearchResponse::class),
        ),
      ],
    ),
    ApiResponse(
      responseCode = "404",
      description = "Nothing exactly matches all the search terms",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ExactMatchSearchResponse::class),
        ),
      ],
    ),
  )
  @PostMapping("/person/search/exact-match")
  @PreAuthorize("hasRole('$API_READ_ONLY')")
  fun search(@Valid @RequestBody body: ExactMatchSearchRequest): ExactMatchSearchResponse {
    val matchIds = personMatchClient.exactMatchSearch(body).matchIds
    val persons = matchIds.map { personRepository.findByMatchId(UUID.fromString(it))!! }
    return ExactMatchSearchResponse.from(persons)
  }
}
