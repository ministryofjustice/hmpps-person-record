package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminDeleteRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.DeletionService
import uk.gov.justice.digital.hmpps.personrecord.service.message.PersonToDeleteNotFoundException

@RestController
class DeleteController(
  private val deleteService: DeletionService,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @PostMapping("/admin/delete")
  suspend fun postDelete(
    @RequestBody records: List<AdminDeleteRecord>,
  ) {
    records.forEach { r ->
      try {
        deleteService.processDelete {
          personRepository.findByCrn(r.sourceSystemId)
        }
      } catch (e: PersonToDeleteNotFoundException) {
        log.error("Could not find person to delete with crn ${r.sourceSystemId}")
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
