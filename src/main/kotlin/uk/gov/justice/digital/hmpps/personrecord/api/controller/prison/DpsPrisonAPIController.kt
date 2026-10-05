package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.DpsPrisonGetHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.DpsPrisonRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonEmailAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonEmailAddressRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonPhoneNumberRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionInsertRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison/dps")
class DpsPrisonAPIController(
  private val dpsPrisonGetHandler: DpsPrisonGetHandler,
  private val prisonReligionInsertHandler: PrisonReligionInsertHandler,
) {

  @Operation(
    description = "**NOTE: Use this only if you want to retrieve Prison Religion history & Alias References.**\n\n" +
      "Retrieve person record by Prison Number. Role required is **$API_READ_ONLY** . " +
      "For Identifiers the crn, prisonNumber, defendantId, cids come from all records related to this person. " +
      "The other Identifiers come from just this person " +
      "**cprUUID is not supplied on this endpoint.** " +
      "In addition to the person data being returned, the response also includes a list of the prison religions associated with the person " +
      "and a prison specific representation of alias & identifiers.",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
    ApiResponse(
      responseCode = "301",
      description = "Permanent Redirect",
      content = [
        Content(schema = Schema(hidden = true)),
      ],
    ),
  )
  fun getByPrisonNumberDps(@PathVariable(name = "prisonNumber") prisonNumber: String): ResponseEntity<DpsPrisonRecord> = dpsPrisonGetHandler.get(prisonNumber)

  @Operation(
    description = "Retrieve prison religion history by Prison Number. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/religion-history")
  fun getReligionHistoryByPrisonNumberDps(@PathVariable(name = "prisonNumber") prisonNumber: String): List<PrisonReligion> = dpsPrisonGetHandler.getReligionHistory(prisonNumber)

  @Operation(
    description = "Update the prisoner's religion by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/religion")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateReligionHistoryByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
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

  @Operation(
    description = "Retrieve the prisoner's email addresses by Prison Number. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/email-addresses")
  fun getEmailAddressesByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
  ): ResponseEntity<List<PrisonEmailAddress>> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Add an email address for the prisoner by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/email-addresses")
  @ResponseStatus(HttpStatus.CREATED)
  fun createEmailAddressByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @Valid @RequestBody request: PrisonEmailAddressRequest,
  ): ResponseEntity<PrisonEmailAddress> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Update an email address for the prisoner by Prison Number and email address id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/email-addresses/{contactId}")
  fun updateEmailAddressByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @PathVariable(name = "contactId") @Parameter(description = "The contact id") contactId: String,
    @Valid @RequestBody request: PrisonEmailAddressRequest,
  ): ResponseEntity<PrisonEmailAddress> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Retrieve the prisoner's phone numbers by Prison Number. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/phone-numbers")
  fun getPhoneNumbersByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
  ): ResponseEntity<List<PrisonPhoneNumber>> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Add a phone number for the prisoner by Prison Number. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PostMapping("/{prisonNumber}/phone-numbers")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPhoneNumberByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @Valid @RequestBody request: PrisonPhoneNumberRequest,
  ): ResponseEntity<PrisonPhoneNumber> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

  @Operation(
    description = "Update a phone number for the prisoner by Prison Number and phone number id. Role required is **$PRISON_API_READ_WRITE**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
  @PutMapping("/{prisonNumber}/phone-numbers/{contactId}")
  fun updatePhoneNumberByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
    @PathVariable(name = "contactId") @Parameter(description = "The contact id") contactId: String,
    @Valid @RequestBody request: PrisonPhoneNumberRequest,
  ): ResponseEntity<PrisonPhoneNumber> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
}
