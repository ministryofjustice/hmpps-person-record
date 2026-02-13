package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.service.PersonExclusionService

@Hidden
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_WRITE}')")
class PersonExclusionController(private val personExclusionService: PersonExclusionService) {

  @PostMapping("/admin/exclusion/person/{prisonNumber}")
  @Transactional
  fun exclude(@NotBlank @PathVariable(name = "prisonNumber") prisonNumber: String): String {
    personExclusionService.exclude(prisonNumber)
    return "OK"
  }
}