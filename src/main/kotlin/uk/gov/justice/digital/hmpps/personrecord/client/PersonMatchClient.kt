package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.MatchStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchMigrateRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse

@FeignClient(
  name = "person-match",
  url = "\${person-match.base-url}",
)
interface PersonMatchClient {

  @PostMapping("/is-cluster-valid")
  fun isClusterValid(@RequestBody requestBody: IsClusterValidRequest): IsClusterValidResponse

  @GetMapping("/person/score/{matchId}")
  fun getPersonScores(@PathVariable matchId: String): List<PersonMatchScore>

  @PostMapping("/person")
  fun postPerson(@RequestBody personMatchRecord: PersonMatchRecord)

  @DeleteMapping("/person")
  fun deletePerson(@RequestBody personMatchIdentifier: PersonMatchIdentifier)

  @PostMapping("/person/migrate")
  fun postPersonMigrate(@RequestBody personMatchMigrateRequest: PersonMatchMigrateRequest)

  @GetMapping("/health")
  fun getHealth(): MatchStatus?
}
