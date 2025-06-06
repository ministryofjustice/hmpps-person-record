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
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val createUpdateService: CreateUpdateService,
  private val mergeService: MergeService,
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
  fun processEvent(mergeDomainEvent: DomainEvent) {
    val toCrn = mergeDomainEvent.additionalInformation?.targetCrn!!
    val fromCrn = mergeDomainEvent.additionalInformation.sourceCrn!!

    corePersonRecordAndDeliusClient.getProbationCase(toCrn).let {
      val from: PersonEntity? = personRepository.findByCrn(fromCrn)
      val to: PersonEntity = createUpdateService.processPerson(
        Person.from(it),
        shouldReclusterOnUpdate = false,
      ) { personRepository.findByCrn(toCrn) }
      mergeService.processMerge(from, to)
    }
  }
}
