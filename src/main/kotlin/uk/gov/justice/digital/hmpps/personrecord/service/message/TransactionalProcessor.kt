package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@Component
class TransactionalProcessor(
  private val createUpdateService: CreateUpdateService,
  private val retryExecutor: RetryExecutor,
) {

  fun processMessage(person: Person, event: String? = null, callback: () -> PersonEntity?) = runBlocking {
    retryExecutor.runWithRetryDatabase {
      createUpdateService.processPerson(person, event, callback)
    }
  }
}
