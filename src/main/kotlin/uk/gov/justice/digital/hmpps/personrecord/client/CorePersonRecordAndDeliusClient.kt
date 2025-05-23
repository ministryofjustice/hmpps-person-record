package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.ProbationCases
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

@Component
class CorePersonRecordAndDeliusClient(private val corePersonRecordAndDeliusWebClient: WebClient) {

  fun getProbationCase(crn: String): ProbationCase = corePersonRecordAndDeliusWebClient.get()
    .uri("/probation-cases/$crn").retrieve().bodyToMono(ProbationCase::class.java).block()!!

  fun getProbationCases(pageParams: CorePersonRecordAndDeliusClientPageParams): ProbationCases? = corePersonRecordAndDeliusWebClient.get()
    .uri("/all-probation-cases") { it.queryParam("page", pageParams.page).queryParam("size", pageParams.size).queryParam("id", "asc").build() }.retrieve().bodyToMono(ProbationCases::class.java).block()!!
}

class CorePersonRecordAndDeliusClientPageParams(val page: Int, val size: Int)
