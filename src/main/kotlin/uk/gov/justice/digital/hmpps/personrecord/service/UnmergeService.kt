package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.MergeService.Companion.MAX_ATTEMPTS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_SELF_MATCH

@Service
class UnmergeService(
  private val telemetryService: TelemetryService,
  private val matchService: MatchService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  private val retryExceptions = listOf(
    ObjectOptimisticLockingFailureException::class,
    CannotAcquireLockException::class,
    JpaSystemException::class,
    JpaObjectRetrievalFailureException::class,
    DataIntegrityViolationException::class,
    ConstraintViolationException::class,
  )

  @Transactional
  fun processUnmerge(reactivatedPerson: Person, unmergedPerson: Person) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, retryExceptions) {
      processUnmergingOfRecords(reactivatedPerson, unmergedPerson)
    }
  }

  private suspend fun processUnmergingOfRecords(reactivatedPerson: Person, unmergedPerson: Person) {
    collectSelfMatchScores(reactivatedPerson, unmergedPerson)
  }

  private suspend fun collectSelfMatchScores(reactivatedPerson: Person, unmergedPerson: Person) {
    return coroutineScope {
      async {
        processSelfMatchScore(reactivatedPerson)
      }.await()
      async {
        processSelfMatchScore(unmergedPerson)
      }.await()
    }
  }

  private fun processSelfMatchScore(person: Person) {
    val (isAboveSelfMatchThreshold, selfMatchScore) = matchService.getSelfMatchScore(person)
    person.selfMatchScore = selfMatchScore
    person.isAboveMatchScoreThreshold = isAboveSelfMatchThreshold
    trackEvent(
      CPR_SELF_MATCH,
      person,
      mapOf(
        EventKeys.PROBABILITY_SCORE to selfMatchScore.toString(),
        EventKeys.IS_ABOVE_SELF_MATCH_THRESHOLD to isAboveSelfMatchThreshold.toString(),
      ),
    )
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
      EventKeys.DEFENDANT_ID to person.defendantId,
      EventKeys.CRN to person.crn,
      EventKeys.PRISON_NUMBER to person.prisonNumber,
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }
}