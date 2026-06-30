package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PrisonEventListenerTestBase : MessagingMultiNodeTestBase() {

  fun publishPrisonPrisonerCreatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPrisonerCreated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }

  fun publishPrisonPrisonerUpdatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPrisonerUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }

  fun publishPrisonPrisonerMergedEvent(targetPrisonNumber: String, sourcePrisonNumber: String) {
    publishDomainEvent(
      PrisonPrisonerMerged(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", targetPrisonNumber))),
        additionalInformation = PrisonPrisonerMergedInfo(sourcePrisonNumber = sourcePrisonNumber),
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
