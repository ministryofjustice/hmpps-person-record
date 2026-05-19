package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.queue.discardNotFoundException

@Component
class CorePersonRecordAndDeliusClient(private val corePersonRecordAndDeliusWebClient: WebClient) {

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



fun getProbationCases(@SpringQueryMap params): Mono<ProbationCase> = corePersonRecordAndDeliusWebClient
  .get()
  .uri("/probation-cases/{id}", crn)
  .retrieve()
  .bodyToMono(ProbationCase::class.java)

class CorePersonRecordAndDeliusClientPageParams(val page: Int, val size: Int) {
  val sort: String = "id,asc"
}

}