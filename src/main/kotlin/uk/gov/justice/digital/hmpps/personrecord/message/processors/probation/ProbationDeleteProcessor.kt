package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import jakarta.persistence.OptimisticLockException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.DeletionService

@Component
class ProbationDeleteProcessor(
  private val deletionService: DeletionService,
  private val personRepository: PersonRepository,
) {
  @Retryable(
    backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
    retryFor = [
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
      InternalServerError::class,
      BadGateway::class,
      ServiceUnavailable::class,
      WebClientRequestException::class,
    ],
  )
  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(domainEvent: DomainEvent) {
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }!!.value
    deletionService.processDelete {
      personRepository.findByCrn(crn)
    }
  }
}
