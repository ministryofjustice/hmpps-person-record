package uk.gov.justice.digital.hmpps.personrecord.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.service.PersonRecordService
import java.net.URI
import java.util.*

@RestController
class PersonController(
  private val personRecordService: PersonRecordService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(description = "Search for a person given their unique person identifier")
  @GetMapping("/person/{person-id}")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "401", description = "Unauthorized - role not provided"),
      ApiResponse(responseCode = "403", description = "Forbidden - role not authorised for access"),
      ApiResponse(responseCode = "404", description = "Person Not Found"),
      ApiResponse(responseCode = "400", description = "Invalid UUID provided"),
      ApiResponse(responseCode = "200", description = "OK - Person found"),
    ],
  )
  fun getPersonById(@PathVariable(name = "person-id") personId: String): Person {
    log.debug("Entered getPersonById($personId)")
    val uuid = try {
      UUID.fromString(personId)
    } catch (ex: IllegalArgumentException) {
      throw ValidationException("Invalid UUID provided for Person: $personId")
    }
    return personRecordService.getPersonById(uuid)
  }

  @Operation(description = "Create a person record given the supplied person details")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "401", description = "Unauthorized - role not provided"),
      ApiResponse(responseCode = "403", description = "Forbidden - role not authorised for access"),
      ApiResponse(responseCode = "400", description = "Incorrect person details supplied"),
      ApiResponse(responseCode = "409", description = "Person details already exist"),
      ApiResponse(responseCode = "201", description = "Person created"),
    ],
  )
  @PostMapping("/person")
  fun createPerson(@RequestBody person: Person): ResponseEntity<Person> {
    log.debug("Entered createPerson()")

    val personRecord = personRecordService.createPersonRecord(person)

    val location: URI = ServletUriComponentsBuilder
      .fromCurrentRequest()
      .path("/{person-id}")
      .buildAndExpand(personRecord.personId)
      .toUri()

    return ResponseEntity.created(location).body(personRecord)
  }

  @Operation(description = "Search for a person record given the supplied search parameters")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "401", description = "Unauthorized - role not provided"),
      ApiResponse(responseCode = "403", description = "Forbidden - role not authorised for access"),
      ApiResponse(responseCode = "404", description = "Person Not Found"),
      ApiResponse(responseCode = "400", description = "Invalid parameters provided"),
      ApiResponse(responseCode = "200", description = "OK - Person found"),
    ],
  )
  @PostMapping("/person/search")
  fun searchForPerson(@RequestBody searchRequest: PersonSearchRequest): List<Person> {
    log.debug("Entered searchForPerson($searchRequest)")
    return personRecordService.searchPersonRecords(searchRequest)
  }
}
