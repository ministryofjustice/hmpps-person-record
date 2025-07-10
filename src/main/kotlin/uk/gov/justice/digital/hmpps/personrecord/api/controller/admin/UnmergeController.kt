package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

@RestController
class UnmergeController(
  private val unmergeService: UnmergeService,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @PostMapping("/admin/unmerge")
  suspend fun unmerge(
    @RequestBody adminUnmergeRequest: AdminUnmergeRequest,
  ) {
    val existing = personRepository.findByCrn(adminUnmergeRequest.unmergedCrn)!!
    val reactivated = personRepository.findByCrn(adminUnmergeRequest.reactivatedCrn)!!
    unmergeService.processUnmerge(reactivated, existing)
  }
}

data class AdminUnmergeRequest(val unmergedCrn: String, val reactivatedCrn: String)
