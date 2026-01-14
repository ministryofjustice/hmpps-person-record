package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.util.UUID

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconImmigrationStatusController(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Operation(description = "Update the immigration status by prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @PostMapping("/syscon-sync/immigration-status/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Immigration status updated in CPR",
    ),
  )
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun updateImmigrationStatus(
    @PathVariable @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true) prisonNumber: String,
    @RequestBody prisonImmigrationStatus: PrisonImmigrationStatus,
  ): String {
    personRepository.findByPrisonNumber(prisonNumber)
      ?.processUpdate(prisonImmigrationStatus)
      ?: throw ResourceNotFoundException(prisonNumber)
    return OK
  }

  // NO-OP ENDPOINTS - TO BE REMOVED ONCE SYSCON DO THEIR WORK
  @Operation(description = "No-Op - To be removed")
  @PostMapping("/syscon-sync/immigration-status")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Sexual Orientation created in CPR",
    ),
  )
  @Transactional
  fun createImmigrationStatus(
    @RequestBody immigrationStatus: PrisonImmigrationStatus,
  ): ResponseEntity<PrisonImmigrationStatusResponse> = ResponseEntity(PrisonImmigrationStatusResponse.from(null), HttpStatus.CREATED)

  @Operation(description = "No-Op - To be removed")
  @PutMapping("/syscon-sync/immigration-status/{cprImmigrationStatusId}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Immigration status updated in CPR",
    ),
  )
  @Transactional
  fun updateImmigrationStatus(
    @PathVariable(name = "cprImmigrationStatusId")
    @Parameter(description = "The identifier of the prison immigration status", required = true)
    cprImmigrationStatusId: UUID,
    @RequestBody immigrationStatus: PrisonImmigrationStatus,
  ): String = OK

  private fun PersonEntity.processUpdate(prisonImmigrationStatus: PrisonImmigrationStatus) {
    val person = Person.from(
      this.apply {
        immigrationStatus = prisonImmigrationStatus.interestToImmigration
      },
    )
    personService.processPerson(person) { this }
  }

  companion object {
    private const val OK = "OK"
  }
}
