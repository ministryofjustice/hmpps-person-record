package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.hibernate.StaleObjectStateException
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class TransactionalProcessor(
  private val createUpdateService: CreateUpdateService,
  // private val retryExecutor: RetryExecutor,
) {

  @Retryable(
    retryFor = [
      ObjectOptimisticLockingFailureException::class,
      CannotAcquireLockException::class,
      JpaSystemException::class,
      JpaObjectRetrievalFailureException::class,
      DataIntegrityViolationException::class,
      ConstraintViolationException::class,
      StaleObjectStateException::class,
    ],
  )
  fun processMessage(person: Person, event: String? = null, callback: () -> PersonEntity?) = runBlocking {
    createUpdateService.processPerson(person, event, callback)
  }
}
