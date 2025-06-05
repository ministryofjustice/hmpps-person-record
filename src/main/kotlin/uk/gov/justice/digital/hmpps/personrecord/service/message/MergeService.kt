package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.persistence.OptimisticLockException
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.ClusterMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class MergeService(
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
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
  @Transactional
  fun processMerge(from: PersonEntity?, to: PersonEntity) {
    when {
      fromClusterHasOneRecord(from) -> markClusterAsMerged(from, to)
    }
    merge(from, to)
  }

  private fun markClusterAsMerged(from: PersonEntity?, to: PersonEntity) {
    from?.personKey?.let {
      it.throwIfCircularMerge(to.personKey!!)
      it.markAsMerged(to.personKey!!)
      personKeyRepository.save(it)
      publisher.publishEvent(ClusterMerged(from, to, it))
    }
  }

  private fun merge(from: PersonEntity?, to: PersonEntity) {
    publisher.publishEvent(PersonMerged(from, to))
    from?.let {
      it.throwIfCircularMerge(to)
      it.removePersonKeyLink()
      it.mergeTo(to)
      personRepository.save(it)
      personMatchService.deleteFromPersonMatch(it)
    }
  }

  private fun PersonKeyEntity.throwIfCircularMerge(to: PersonKeyEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun PersonEntity.throwIfCircularMerge(to: PersonEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun fromClusterHasOneRecord(from: PersonEntity?): Boolean = (from?.personKey?.personEntities?.size ?: 0) == 1
}
