package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
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

  @PostMapping(value = ["/prisoner-search/prisoner-numbers"])
  fun getPrisonNumbers(prisonNumbers: PrisonNumbers): List<Prisoner>? = prisonerSearchWebClient.post().uri("/prisoner-search/prisoner-numbers").bodyValue(prisonNumbers)
    .retrieve().bodyToMono(object : ParameterizedTypeReference<List<Prisoner>>() {}).block()
}

class PrisonNumbers(val prisonerNumbers: List<String>)
