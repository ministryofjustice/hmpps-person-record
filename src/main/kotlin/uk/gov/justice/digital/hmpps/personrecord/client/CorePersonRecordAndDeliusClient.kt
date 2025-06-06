package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.client.model.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DiscardableNotFoundException

@Component
class CorePersonRecordAndDeliusClient(private val corePersonRecordAndDeliusWebClient: WebClient) {

  fun getProbationCase(crn: String): ProbationCase = corePersonRecordAndDeliusWebClient.get()
    .uri("/probation-cases/$crn").retrieve().bodyToMono(ProbationCase::class.java)
    .onErrorResume(WebClientResponseException::class.java) {
      if (it.statusCode == NOT_FOUND) {
        throw DiscardableNotFoundException()
      } else {
        Mono.error(it)
      }
    }.block()!!

  fun getProbationCases(pageParams: CorePersonRecordAndDeliusClientPageParams): ProbationCases? = corePersonRecordAndDeliusWebClient.get()
    .uri("/all-probation-cases") { it.queryParam("size", pageParams.size).queryParam("page", pageParams.page).queryParam("sort", "id,asc").build() }.retrieve().bodyToMono(ProbationCases::class.java).block()!!
}

class CorePersonRecordAndDeliusClientPageParams(val page: Int, val size: Int)
