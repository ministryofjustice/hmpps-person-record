package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetryDatabase

@Component
class TransactionalProcessor(
  private val createUpdateService: CreateUpdateService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun processMessage(person: Person, event: String? = null, callback: () -> PersonEntity?): PersonEntity = runBlocking {
    runWithRetryDatabase(MAX_ATTEMPTS, retryDelay) {
      return@runWithRetryDatabase createUpdateService.processPerson(person, event, callback)
    }
  }

  companion object {
    private const val MAX_ATTEMPTS = 5
  }
}
