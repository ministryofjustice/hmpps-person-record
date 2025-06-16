package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.queue.discardNotFoundException

@Component
class CorePersonRecordAndDeliusClient(private val corePersonRecordAndDeliusWebClient: WebClient) {

  fun getPerson(crn: String): Person {
    val probationCase = corePersonRecordAndDeliusWebClient
      .get()
      .uri("/probation-cases/$crn")
      .retrieve()
      .bodyToMono(ProbationCase::class.java)
      .discardNotFoundException()
      .block()!!
    return Person.from(probationCase)
  }

  fun getProbationCases(pageParams: CorePersonRecordAndDeliusClientPageParams): ProbationCases? = corePersonRecordAndDeliusWebClient.get()
    .uri("/all-probation-cases") { it.queryParam("size", pageParams.size).queryParam("page", pageParams.page).queryParam("sort", "id,asc").build() }.retrieve().bodyToMono(ProbationCases::class.java).block()!!
}

class CorePersonRecordAndDeliusClientPageParams(val page: Int, val size: Int)
