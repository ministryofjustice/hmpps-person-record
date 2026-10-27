package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.ReferenceDataWithActiveResponse
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

@Tag(name = "Canonical")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/reference-data/")
class CanonicalReferenceDataController {
  @Operation(
    description = "Retrieve list of religion codes. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/religion-codes")
  fun getReligionCodes(@RequestParam(name = "active", required = false) active: Boolean = true): List<ReferenceDataWithActiveResponse> = ReligionCode.entries.filter { !active || it.current }.map { ReferenceDataWithActiveResponse(it.name, it.description, it.current) }

  @Operation(
    description = "Retrieve list of country codes. Role required is **$API_READ_ONLY**. ",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/country-codes")
  fun getCountryCodes(): List<ReferenceDataResponse> = CountryCode.entries.map { ReferenceDataResponse(it.name, it.description) }
}
