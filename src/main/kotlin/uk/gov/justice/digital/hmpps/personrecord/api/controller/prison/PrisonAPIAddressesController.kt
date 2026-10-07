package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonAddressesHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIAddressesController(
  private val prisonAddressesHandler: PrisonAddressesHandler,
) {
  @Operation(
    description = "Retrieve the prisoner's addresses by Prison Number, most recent first. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/addresses")
  fun getAddressesByPrisonNumberDps(
    @PathVariable(name = "prisonNumber") @Parameter(description = "The prisoner number") prisonNumber: String,
  ): List<CanonicalAddress> = prisonAddressesHandler.get(prisonNumber)
}
