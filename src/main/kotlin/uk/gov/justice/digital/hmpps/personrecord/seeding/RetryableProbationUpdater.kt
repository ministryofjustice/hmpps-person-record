package uk.gov.justice.digital.hmpps.personrecord.seeding

import jakarta.persistence.OptimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import uk.gov.justice.digital.hmpps.personrecord.CprRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class RetryableProbationUpdater(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
) {

  @CprRetryable(
    retryFor = [
      WebClientException::class,
      OptimisticLockException::class,
      DataIntegrityViolationException::class,
      CannotAcquireLockException::class,
    ],
  )
  fun repopulateProbationRecord(pageParams: CorePersonRecordAndDeliusClientPageParams) {
    corePersonRecordAndDeliusClient.getProbationCases(pageParams)
      ?.cases?.forEach { case ->
        val person = Person.from(case)
        personRepository.findByCrn(person.crn!!).exists(
          no = {
            log.error("CRN not found in Database ${person.crn}")
          },
          yes = {
            if (it.isNotMerged()) {
              it.update(person)
              personRepository.save(it)
            }
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
