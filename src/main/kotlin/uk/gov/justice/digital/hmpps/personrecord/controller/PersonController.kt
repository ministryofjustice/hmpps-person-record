package uk.gov.justice.digital.hmpps.personrecord.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDTO
import uk.gov.justice.digital.hmpps.personrecord.service.PersonRecordService

@RestController
class PersonController(
  private val personRecordService: PersonRecordService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping("/person/{person-id}")
  fun getPersonDetailsById(@PathVariable(name = "person-id") personId: String): PersonDTO {
    log.debug("Entered getPersonDetailsById($personId)")
    return personRecordService.getPersonById(personId)
  }
}
