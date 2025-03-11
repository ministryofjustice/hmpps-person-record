package uk.gov.justice.digital.hmpps.personrecord.service.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@Component
class PersonMatchService(
  private val personMatchClient: PersonMatchClient,
  private val retryExecutor: RetryExecutor,
  @Value("\${CALL_PERSON_MATCH}")
  private var callPersonMatch: Boolean,
) {

  // Fire and forget, until consumed in CPR-585
  fun getScores(personEntity: PersonEntity) {
    if (callPersonMatch) {
      CoroutineScope(Dispatchers.Default).launch {
        retryExecutor.runWithRetryHTTPWith404s { personMatchClient.getPersonScores(personEntity.matchId.toString()) }
      }
    }
  }
}
