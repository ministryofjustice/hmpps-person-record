package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.PersonDeletionService

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('$PERSON_RECORD_SYSCON_SYNC_WRITE')")
class PrisonAPIDeleteController(
  private val personDeletionService: PersonDeletionService,
  private val personRepository: PersonRepository,
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
    ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content = [
        Content(schema = Schema(hidden = true)),
      ],
    ),
    ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content = [
        Content(schema = Schema(hidden = true)),
      ],
    ),
  )
  @DeleteMapping("/person/prison/{prisonNumber}")
  fun deleteByPrisonNumber(@PathVariable(name = "prisonNumber") prisonNumber: String): ResponseEntity<Unit> {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException("Person '$prisonNumber' does not exist")
    personDeletionService.processDelete(personEntity)
    return ResponseEntity.ok().build()
  }
}
