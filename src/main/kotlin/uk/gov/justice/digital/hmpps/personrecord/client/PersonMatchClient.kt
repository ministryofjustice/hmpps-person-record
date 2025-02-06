package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRequest

@FeignClient(
  name = "person-match",
  url = "\${person-match.base-url}",
)
interface PersonMatchClient {

  @PostMapping("/person/migrate")
  fun postPersonMigrate(@RequestBody personMatchRequest: PersonMatchRequest)
}
