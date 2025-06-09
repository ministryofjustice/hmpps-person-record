package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DiscardableNotFoundException

@Component
class PrisonerSearchClient(private val prisonerSearchWebClient: WebClient) {

  fun getPrisoner(prisonNumber: String): Prisoner? = prisonerSearchWebClient
    .get()
    .uri("/prisoner/$prisonNumber")
    .retrieve()
    .bodyToMono(Prisoner::class.java)
    .onErrorResume(WebClientResponseException::class.java) {
      if (it.statusCode == NOT_FOUND) {
        throw DiscardableNotFoundException()
      } else {
        Mono.error(it)
      }
    }.block()

  @PostMapping(value = ["/prisoner-search/prisoner-numbers"])
  fun getPrisonNumbers(prisonNumbers: PrisonNumbers): List<Prisoner>? = prisonerSearchWebClient.post().uri("/prisoner-search/prisoner-numbers").bodyValue(prisonNumbers)
    .retrieve().bodyToMono(object : ParameterizedTypeReference<List<Prisoner>>() {}).block()
}

class PrisonNumbers(val prisonerNumbers: List<String>)
