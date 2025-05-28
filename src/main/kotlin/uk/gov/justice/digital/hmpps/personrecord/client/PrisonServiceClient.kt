package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PrisonNumbers

@Component
class PrisonServiceClient(private val prisonServiceWebClient: WebClient) {

  fun getPrisonNumbers(pageParams: PageParams): PrisonNumbers? = prisonServiceWebClient.get()
    .uri("/api/prisoners/prisoner-numbers") { it.queryParam("size", pageParams.size).queryParam("page", pageParams.page).build() }
    .retrieve().bodyToMono(PrisonNumbers::class.java).block()!!
}

class PageParams(val page: Int, val size: Int)
