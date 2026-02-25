package uk.gov.justice.digital.hmpps.personrecord.api.controller.court

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchDetailsResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@RestController
@RequestMapping("/person/commonplatform")
@PreAuthorize("hasRole('$API_READ_ONLY')")
class PersonCommonPlatformController(
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
) {

  @GetMapping("/{defendantId}/match-details")
  fun getMatchDetails(@PathVariable defendantId: String): ResponseEntity<MatchDetailsResponse> {
    val personEntity = personRepository.findByDefendantId(defendantId)
      ?: return ResponseEntity.notFound().build()

    val status = personMatchService.determineMatchStatus(personEntity)
    return ResponseEntity.ok(MatchDetailsResponse(matchStatus = status))
  }
}
