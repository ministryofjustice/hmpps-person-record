package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.service.queue.discardNotFoundException

@Component
class PrisonerSearchClient(private val prisonerSearchWebClient: WebClient) {

  fun getPrisoner(prisonNumber: String): Prisoner? = prisonerSearchWebClient
    .get()
    .uri("/prisoner/$prisonNumber")
    .retrieve()
    .bodyToMono(Prisoner::class.java)
    .discardNotFoundException()
    .block()
}
