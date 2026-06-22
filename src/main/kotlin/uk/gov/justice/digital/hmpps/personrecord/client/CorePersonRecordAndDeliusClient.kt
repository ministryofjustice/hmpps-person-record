package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
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
    .bodyToMono<ProbationCase>()

  fun getAddress(deliusAddressId: Long): Address? = Address.from(
    corePersonRecordAndDeliusWebClient
      .get()
      .uri("/address/{id}", deliusAddressId)
      .retrieve()
      .bodyToMono<ProbationAddress>()
      .block()!!,
  )
}
