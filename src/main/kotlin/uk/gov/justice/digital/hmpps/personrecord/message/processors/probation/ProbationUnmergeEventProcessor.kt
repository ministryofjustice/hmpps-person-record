package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.UnmergeEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.person.UnmergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED

@Component
class ProbationUnmergeEventProcessor(
  private val telemetryService: TelemetryService,
  private val unmergeService: UnmergeService,
  private val personRepository: PersonRepository,
  private val encodingService: EncodingService,
) {

  fun processEvent(domainEvent: DomainEvent) {
    telemetryService.trackEvent(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf(
        EventKeys.REACTIVATED_CRN to domainEvent.additionalInformation?.reactivatedCrn,
        EventKeys.UNMERGED_CRN to domainEvent.additionalInformation?.unmergedCrn,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to DELIUS.name,
      ),
    )

    val (unmergedProbationCase, reactivatedProbationCase) = collectProbationCases(domainEvent)
    unmergeService.processUnmerge(
      UnmergeEvent(
        event = domainEvent.eventType,
        reactivatedRecord = Person.from(reactivatedProbationCase),
        reactivatedSystemId = Pair(EventKeys.REACTIVATED_CRN, reactivatedProbationCase.identifiers.crn!!),
        unmergedRecord = Person.from(unmergedProbationCase),
        unmergedSystemId = Pair(EventKeys.UNMERGED_CRN, unmergedProbationCase.identifiers.crn!!),
      ),
      reactivatedPersonCallback = {
        personRepository.findByCrn(reactivatedProbationCase.identifiers.crn)
      },
      unmergedPersonCallback = {
        personRepository.findByCrn(unmergedProbationCase.identifiers.crn)
      },
    )
  }

  private fun collectProbationCases(domainEvent: DomainEvent): Pair<ProbationCase, ProbationCase> = runBlocking {
    val deferredUnmergedCRNRequest = async { encodingService.getProbationCase(domainEvent.additionalInformation?.unmergedCrn!!) }
    val deferredReactivatedCRNRequest = async { encodingService.getProbationCase(domainEvent.additionalInformation?.reactivatedCrn!!) }
    val unmergedProbationCase = deferredUnmergedCRNRequest.await().getOrThrow()
    val reactivatedProbationCase = deferredReactivatedCRNRequest.await().getOrThrow()
    return@runBlocking Pair(
      unmergedProbationCase!!,
      reactivatedProbationCase!!,
    )
  }
}
