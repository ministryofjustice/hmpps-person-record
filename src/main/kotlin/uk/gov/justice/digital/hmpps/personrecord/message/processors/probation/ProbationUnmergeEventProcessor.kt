package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.UnmergeService

@Component
class ProbationUnmergeEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val unmergeService: UnmergeService,
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
  fun processEvent(domainEvent: DomainEvent) {
    val existingPerson = getProbationPerson(domainEvent.additionalInformation?.unmergedCrn!!, true)
    val reactivatedPerson = getProbationPerson(domainEvent.additionalInformation.reactivatedCrn!!, false)
    unmergeService.processUnmerge(reactivatedPerson, existingPerson)
  }

  private fun getProbationPerson(crn: String, shouldLinkOnCreate: Boolean): PersonEntity = corePersonRecordAndDeliusClient.getProbationCase(crn)
    .let {
      createUpdateService.processPerson(
        Person.from(it),
        shouldLinkOnCreate = shouldLinkOnCreate,
      ) { personRepository.findByCrn(it.identifiers.crn!!) }
    }
}
