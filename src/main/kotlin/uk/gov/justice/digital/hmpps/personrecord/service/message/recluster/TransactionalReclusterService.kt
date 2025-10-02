package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Component
class TransactionalReclusterService(
  private val reclusterService: ReclusterService,
) {

  @Retryable( maxAttempts = 2,
    backoff = Backoff(delay = 200, random = true, multiplier = 3.0),
    retryFor = [
      WebClientException::class,
    ])
  @Transactional
  fun triggerRecluster(person: PersonEntity) = reclusterService.recluster(person)
}
