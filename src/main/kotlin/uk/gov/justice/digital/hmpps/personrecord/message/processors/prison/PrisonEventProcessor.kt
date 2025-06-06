package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class PrisonEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @Retryable(
    backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
    retryFor = [
      InternalServerError::class,
      BadGateway::class,
      ServiceUnavailable::class,
      WebClientRequestException::class,
    ],
  )
  fun processEvent(domainEvent: DomainEvent) {
    val prisonNumber = domainEvent.additionalInformation?.prisonNumber!!
    prisonerSearchClient.getPrisoner(prisonNumber)?.let {
      createUpdateService.processPerson(Person.from(it)) {
        personRepository.findByPrisonNumber(prisonNumber)
      }
    }
  }
}
