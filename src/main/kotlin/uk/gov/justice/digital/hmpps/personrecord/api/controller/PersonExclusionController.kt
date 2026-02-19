package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.PersonExclusionService

@Hidden
@RestController
class PersonExclusionController(
  private val personRepository: PersonRepository,
  private val personExclusionService: PersonExclusionService,
) {

  @PostMapping("/admin/exclusion/prisoner")
  fun excludePrisoner(@RequestBody exclusionRequest: ExclusionRequest): String {
    personExclusionService.exclude { personRepository.findByPrisonNumber(exclusionRequest.personId) }
    return "OK"
  }

  data class ExclusionRequest(
    val personId: String,
  )
}
