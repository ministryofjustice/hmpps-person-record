package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Address
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerDetails
import uk.gov.justice.digital.hmpps.personrecord.config.PrisonServiceOAuth2Config

@FeignClient(
  name = "prison-service",
  url = "\${prison-service.base-url}",
  configuration = [PrisonServiceOAuth2Config::class],
)
interface PrisonServiceClient {

  @GetMapping(value = ["\${prison-service.prisoner-details}/{prisonerNumber}"])
  fun getPrisonerDetails(@PathVariable("prisonerNumber") prisonerNumber: String): PrisonerDetails?

  @GetMapping(value = ["\${prison-service.prisoner-details}/{prisonerNumber}/addresses"])
  fun getPrisonerAddresses(@PathVariable("prisonerNumber") prisonerNumber: String): List<Address>?
}
