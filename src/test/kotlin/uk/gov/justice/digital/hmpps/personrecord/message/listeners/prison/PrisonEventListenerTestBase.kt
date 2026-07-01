package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PrisonEventListenerTestBase : MessagingMultiNodeTestBase() {

  fun publishPrisonPrisonerCreatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPersonCreated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }

  fun publishPrisonPrisonerUpdatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPersonUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }

  fun publishPrisonPrisonerMergedEvent(targetPrisonNumber: String, sourcePrisonNumber: String) {
    publishDomainEvent(
      PrisonPersonMerged(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", targetPrisonNumber))),
        additionalInformation = PrisonPersonMergedInfo(sourcePrisonNumber = sourcePrisonNumber),
      ),
    )
  }

  fun prisonCreateEventAndResponseSetup(
    apiResponseSetup: ApiResponseSetup,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubPrisonResponse(
      apiResponseSetup,
      scenario,
      currentScenarioState,
      nextScenarioState,
    )

    publishPrisonPrisonerCreatedEvent(apiResponseSetup.prisonNumber!!)
  }

  fun prisonUpdateEventAndResponseSetup(
    apiResponseSetup: ApiResponseSetup,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubPrisonResponse(
      apiResponseSetup,
      scenario,
      currentScenarioState,
      nextScenarioState,
    )

    publishPrisonPrisonerCreatedEvent(apiResponseSetup.prisonNumber!!)
  }

  fun prisonMergeEventAndResponseSetup(
    sourcePrisonNumber: String,
    targetPrisonNumber: String,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubPrisonResponse(
      ApiResponseSetup(prisonNumber = targetPrisonNumber),
      scenario,
      currentScenarioState,
      nextScenarioState,
    )

    publishPrisonPrisonerMergedEvent(targetPrisonNumber, sourcePrisonNumber)
  }
}
