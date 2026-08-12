package uk.gov.justice.digital.hmpps.personrecord.api.controller.court

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_SEARCH_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.handler.search.CandidateSearchHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord

@Tag(name = "Court")
@RestController
@RequestMapping("/person/commonplatform")
@PreAuthorize("hasRole('$API_SEARCH_ONLY')")
@Profile("!prod")
class CommonPlatformCandidateSearchController(
  private val candidateSearchHandler: CandidateSearchHandler,
) {

  @Operation(
    description = """Retrieve candidate matches for a person by Defendant ID. Role required is **$API_SEARCH_ONLY** .
      The response will be a list of CanonicalRecords for each candidate match, sorted by match weight in descending order.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{defendantId}/candidate-matches")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CanonicalRecord::class),
        ),
      ],
    ),
  )
  fun searchCandidates(@PathVariable defendantId: String): List<CanonicalRecord> = candidateSearchHandler.searchByDefendantId(defendantId)
}
