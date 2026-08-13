package uk.gov.justice.digital.hmpps.personrecord.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.web.PagedModel.PageMetadata
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.CprRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class CorePersonRecordAndDeliusClient(private val corePersonRecordAndDeliusWebClient: WebClient) {

  @CprRetryable(retryFor = [WebClientResponseException.NotFound::class])
  fun getPerson(crn: String): Person = Person.from(getProbationCase(crn))

  fun getProbationCase(crn: String): ProbationCase = fetchProbationCase(crn).block()!!

  private fun fetchProbationCase(crn: String): Mono<ProbationCase> = corePersonRecordAndDeliusWebClient
    .get()
    .uri("/probation-cases/{id}", crn)
    .retrieve()
    .bodyToMono<ProbationCase>()

  fun getAddress(deliusAddressId: Long): Address? = Address.from(
    corePersonRecordAndDeliusWebClient
      .get()
      .uri("/address/{id}", deliusAddressId)
      .retrieve()
      .bodyToMono<ProbationAddress>()
      .block()!!,
  )

  fun getProbationCases(params: CorePersonRecordAndDeliusClientPageParams): ProbationCases? = corePersonRecordAndDeliusWebClient
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
    .bodyToMono<ProbationCases>()
    .block()
}

class CorePersonRecordAndDeliusClientPageParams(val page: Long, val size: Int) {
  val sort: String = "id,asc"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCases(
  val page: PageMetadata,
)
