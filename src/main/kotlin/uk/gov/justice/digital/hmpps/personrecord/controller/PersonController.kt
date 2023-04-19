package uk.gov.justice.digital.hmpps.personrecord.controller

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDTO
import uk.gov.justice.digital.hmpps.personrecord.service.PersonRecordService
import java.net.URI
import java.util.UUID

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
    val uuid = try {
      UUID.fromString(personId)
    } catch (ex: IllegalArgumentException) {
      throw ValidationException("Invalid UUID provided for Person: $personId")
    }
    return personRecordService.getPersonById(uuid)
  }

  @PostMapping("/person")
  fun createPerson(@RequestBody person: PersonDTO): ResponseEntity<PersonDTO> {
    log.debug("Entered createPerson()")

    val personRecord = personRecordService.createPersonRecord(person)

    val location: URI = ServletUriComponentsBuilder
      .fromCurrentRequest()
      .path("/{person-id}")
      .buildAndExpand(personRecord.personId)
      .toUri()

    return ResponseEntity.created(location).body(personRecord)
  }
}
