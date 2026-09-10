package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonPerson
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor2
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
// qqRP @Profile("dev")
class PrisonAPICreateController(
  private val prisonEventProcessor: PrisonEventProcessor2
  ,
) {
  @Operation(description = "Save the prison person. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/prison/person/{prisonNumber}")
  fun create(
    @PathVariable prisonNumber: String,
    @Valid @RequestBody person: PrisonPerson,
  ) {
    prisonEventProcessor.processEvent(prisonNumber, person)
    val qqrp = 1;
  }
}
