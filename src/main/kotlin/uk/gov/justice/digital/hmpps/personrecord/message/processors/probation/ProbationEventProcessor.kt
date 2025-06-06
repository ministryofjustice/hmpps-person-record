package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class ProbationEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
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
  fun processEvent(crn: String) {
    corePersonRecordAndDeliusClient.getProbationCase(crn).let {
      createUpdateService.processPerson(Person.from(it)) {
        personRepository.findByCrn(crn)
      }
    }
  }
}
