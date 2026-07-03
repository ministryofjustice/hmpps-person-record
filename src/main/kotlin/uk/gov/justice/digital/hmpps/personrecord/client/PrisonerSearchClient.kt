package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.queue.discardNotFoundException

@Component
class PrisonerSearchClient(private val prisonerSearchWebClient: WebClient) {

  fun getPrisoner(prisonNumber: String): Person? = prisonerSearchWebClient
    .get()
    .uri("/prisoner/{id}", prisonNumber)
    .retrieve()
    .bodyToMono<Prisoner>()
    .discardNotFoundException()
    .block()?.let { return Person.from(it) }
}
