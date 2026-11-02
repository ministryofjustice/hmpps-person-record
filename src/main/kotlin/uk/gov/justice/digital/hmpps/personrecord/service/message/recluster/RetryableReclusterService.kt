package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import jakarta.persistence.OptimisticLockException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class RetryableReclusterService(
  private val reclusterService: ReclusterService,
  private val personRepository: PersonRepository,
) {

  @Retryable(
    maxAttempts = 2,
    backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
    retryFor = [
      WebClientException::class,
      OptimisticLockingFailureException::class,
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  fun triggerRecluster(person: PersonEntity) {
    val person = personRepository.findById(person.id).orElseThrow()
    reclusterService.recluster(person)
  }
}
