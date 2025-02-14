package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchMigrateRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord

@FeignClient(
  name = "person-match",
  url = "\${person-match.base-url}",
)
interface PersonMatchClient {

  @PostMapping("/person")
  fun postPerson(@RequestBody personMatchRecord: PersonMatchRecord)

  @DeleteMapping("/person")
  fun deletePerson(@RequestBody personMatchRecord: PersonMatchRecord)

  @PostMapping("/person/migrate")
  fun postPersonMigrate(@RequestBody personMatchMigrateRequest: PersonMatchMigrateRequest)
}
