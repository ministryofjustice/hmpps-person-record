package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DiscardableNotFoundException

@Component
class RetryableProbationUpdater(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
) {

  @Retryable(
    maxAttempts = 2,
    backoff = Backoff(delay = 200, random = true, multiplier = 2.0),
    retryFor = [
      WebClientException::class,
    ],
  )
  fun repopulateProbationRecord(pageParams :CorePersonRecordAndDeliusClientPageParams ) {
    corePersonRecordAndDeliusClient.getProbationCases(pageParams)
      ?.cases?.forEach {
        val person = Person.from(it)
        personRepository.findByCrn(person.crn!!).exists(
          no = {
            log.error("CRN not found in Database ${person.crn}")
          },
          yes = {
            it.update(person)
            personRepository.save(it)
          },
        )
      }
  }

  private fun PersonEntity?.exists(no: () -> Unit, yes: (personEntity: PersonEntity) -> Unit) = when {
    this == null -> no()
    else -> yes(this)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
