package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionGetHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionInsertRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison/")
class PrisonAPIReligionController(
  private val prisonReligionGetHandler: PrisonReligionGetHandler,
  private val prisonReligionInsertHandler: PrisonReligionInsertHandler,
) {
  @Operation(
    description = "Retrieve prison religion history by Prison Number. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/religion-history")
  fun getReligionHistoryByPrisonNumber(@PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String): List<PrisonReligion> = prisonReligionGetHandler.getReligionHistory(prisonNumber)

  @Operation(
    description = "Update the prisoner's religion by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/religion")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateReligionHistoryByPrisonNumber(
    @PathVariable(name = "prisonNumber") @Parameter(example = "A1234AA") prisonNumber: String,
    @RequestBody insertRequest: PrisonReligionInsertRequest,
  ) {
    prisonReligionInsertHandler.handleCprInsert(prisonNumber, insertRequest)
  }

  @Operation(
    description = "Retrieve list of prison religion codes. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/religion-codes")
  fun getReligionCodes(@RequestParam(name = "active", required = false) active: Boolean = true): List<ReferenceDataResponse> = ReligionCode.entries.filter { !active || it.current }.map { ReferenceDataResponse(it.name, it.description, it.current) }
}
