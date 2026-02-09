package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.CoreAddressInsertService

@RestController
class SysconSyncAddressInsertController(
  private val coreAddressInsertService: CoreAddressInsertService
) {

  @PostMapping("/syscon-sync/address/{prisonNumber}")
  fun save(
    @NotBlank @PathVariable(name = "prisonNumber") prisonNumber: String,
    @RequestBody sysconSpecificRequest: SysconAddressInsertReq,
  ): ResponseEntity<String> {

    val coreAddressModel = Address()
    val coreAddress = coreAddressInsertService.handle(coreAddressModel)

    return ResponseEntity
      .status(201)
      .body("Some custom json response for syscon")
  }

  data class SysconAddressInsertReq(
    val street: String,
    val city: String,
  )
}