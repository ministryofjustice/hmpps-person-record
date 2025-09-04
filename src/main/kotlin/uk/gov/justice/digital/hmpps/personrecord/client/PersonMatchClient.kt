package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.service.queue.discardNotFoundException

@Component
class PersonMatchClient(private val personMatchWebClient: WebClient) {

  fun isClusterValid(requestBody: List<String>): IsClusterValidResponse = personMatchWebClient
    .post()
    .uri("/is-cluster-valid")
    .bodyValue(requestBody)
    .retrieve()
    .bodyToMono(IsClusterValidResponse::class.java)
    .block()!!

  fun getPersonScores(matchId: String): List<PersonMatchScore> = personMatchWebClient
    .get()
    .uri("/person/score/$matchId")
    .retrieve()
    .bodyToMono<List<PersonMatchScore>>()
    .block()!!

  fun postPerson(personMatchRecord: PersonMatchRecord) = personMatchWebClient
    .post()
    .uri("/person")
    .bodyValue(personMatchRecord)
    .retrieve()
    .toBodilessEntity()
    .block()

  fun deletePerson(personMatchIdentifier: PersonMatchIdentifier) = personMatchWebClient
    .method(HttpMethod.DELETE)
    .uri("/person")
    .bodyValue(personMatchIdentifier)
    .retrieve()
    .toBodilessEntity()
    .discardNotFoundException()
    .block()
}
