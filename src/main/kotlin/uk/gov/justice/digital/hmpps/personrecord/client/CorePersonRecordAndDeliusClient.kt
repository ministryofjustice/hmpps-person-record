package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.seeding.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.service.queue.discardNotFoundException

@Component
class CorePersonRecordAndDeliusClient(private val corePersonRecordAndDeliusWebClient: WebClient, private val migrationClient: WebClient) {

  fun getPerson(crn: String): Person {
    val probationCase = getProbationCase(crn)
      .discardNotFoundException()
      .block()!!
    return Person.from(probationCase)
  }

  fun getPersonErrorIfNotFound(crn: String): Person {
    val probationCase = getProbationCase(crn)
      .block()!!
    return Person.from(probationCase)
  }

  private fun getProbationCase(crn: String): Mono<ProbationCase> = corePersonRecordAndDeliusWebClient
    .get()
    .uri("/probation-cases/{id}", crn)
    .retrieve()
    .bodyToMono(ProbationCase::class.java)

  fun getProbationCases(params: CorePersonRecordAndDeliusClientPageParams): ProbationCases? = migrationClient
    .get()
    .uri { uriBuilder ->
      uriBuilder
        .path("/all-probation-cases")
        .queryParam("page", params.page)
        .queryParam("size", params.size)
        .queryParam("sort", params.sort)
        .build()
    }
    .retrieve()
    .bodyToMono(ProbationCases::class.java)
    .block()
}

class CorePersonRecordAndDeliusClientPageParams(val page: Long, val size: Int) {
  val sort: String = "id,asc"
}
