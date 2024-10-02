package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.UnmergeService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.UNMERGE_MESSAGE_RECEIVED

@Component
class ProbationUnmergeEventProcessor(
  val telemetryService: TelemetryService,
  val unmergeService: UnmergeService,
) : BaseProbationEventProcessor() {

  fun processEvent(domainEvent: DomainEvent) = runBlocking {
    telemetryService.trackEvent(
      UNMERGE_MESSAGE_RECEIVED,
      mapOf(
        EventKeys.REACTIVATED_CRN to domainEvent.additionalInformation?.reactivatedCRN,
        EventKeys.UNMERGED_CRN to domainEvent.additionalInformation?.unmergedCRN,
        EventKeys.EVENT_TYPE to domainEvent.eventType,
        EventKeys.SOURCE_SYSTEM to DELIUS.name,
      ),
    )
    val deferredUnmergedCRNRequest = async { getProbationCase(domainEvent.additionalInformation?.unmergedCRN!!) }
    val deferredReactivatedCRNRequest = async { getProbationCase(domainEvent.additionalInformation?.unmergedCRN!!) }

    val unmergedProbationCase = deferredUnmergedCRNRequest.await().getOrThrow()
    val reactivatedProbationCase = deferredReactivatedCRNRequest.await().getOrThrow()

    unmergeService.processUnmerge(
      reactivatedPerson = Person.from(reactivatedProbationCase!!),
      unmergedPerson = Person.from(unmergedProbationCase!!),
    )
  }
}
