package uk.gov.justice.digital.hmpps.personrecord.config

import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.COMMON_PLATFORM_HEARING
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.MessageType.LIBRA_COURT_CASE
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderUnMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderUnMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderUpdated
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.queue.LARGE_CASE_EVENT_TYPE
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import uk.gov.justice.hmpps.sqs.publish
import java.time.Instant
import java.util.UUID

abstract class MessagingTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
  }

  internal val courtEventsTopic by lazy {
    hmppsQueueService.findByTopicId("courtcasestopic")
  }

  val courtEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.COURT_CASES_QUEUE_ID)
  }

  val testOnlyCourtEventsQueue by lazy {
    hmppsQueueService.findByQueueId("testcourtcasesqueue")
  }

  val testOnlyCPRDomainEventsQueue by lazy {
    hmppsQueueService.findByQueueId("testcprdomaineventsqueue")
  }

  val probationEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_EVENT_QUEUE_ID)
  }

  val probationMergeEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_MERGE_EVENT_QUEUE_ID)
  }

  val probationDeleteEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_DELETION_EVENT_QUEUE_ID)
  }

  val prisonEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PRISON_EVENT_QUEUE_ID)
  }

  val prisonMergeEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PRISON_MERGE_EVENT_QUEUE_ID)
  }

  val sasEventsQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.SAS_EVENT_QUEUE_ID)
  }

  val probationAddressEventsFromCPRQueue by lazy {
    hmppsQueueService.findByQueueId(Queues.PROBATION_ADDRESS_EVENT_FROM_CPR_QUEUE)
  }

  internal fun publishLibraMessage(message: String) = publishCourtMessage(message, LIBRA_COURT_CASE, "libra.case.received", null)

  internal fun publishCommonPlatformMessage(message: String) = publishCourtMessage(message, COMMON_PLATFORM_HEARING, "commonplatform.case.received", "ConfirmedOrUpdated")

  internal fun publishLargeCommonPlatformMessage(message: String) = publishCourtMessage(message, COMMON_PLATFORM_HEARING, LARGE_CASE_EVENT_TYPE, "ConfirmedOrUpdated")

  private fun publishCourtMessage(
    message: String,
    messageType: MessageType,
    eventType: String,
    hearingEventType: String?,
  ) {
    val attributes = mutableMapOf(
      "messageType" to MessageAttributeValue.builder().dataType("String")
        .stringValue(messageType.name).build(),
      "eventType" to MessageAttributeValue.builder().dataType("String")
        .stringValue(eventType).build(),
      "messageId" to MessageAttributeValue.builder().dataType("String")
        .stringValue(UUID.randomUUID().toString()).build(),
    )
    hearingEventType?.let {
      val hearingEventTypeValue =
        MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(hearingEventType)
          .build()
      attributes.put("hearingEventType", hearingEventTypeValue)
    }
    courtEventsTopic?.publish(
      eventType = messageType.name,
      event = message,
      attributes = attributes,
      messageGroupId = messageType.name,
    )

    expectNoMessagesOn(courtEventsQueue)
  }

  fun expectOneMessageOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
    } matches { it == 1 }
  }

  fun expectNoMessagesOn(queue: HmppsQueue?) {
    await untilCallTo {
      queue?.sqsClient?.countMessagesOnQueue(queue.queueUrl)?.get()
    } matches { it == 0 }
  }

  fun expectNoMessagesOnQueueOrDlq(queue: HmppsQueue?) {
    expectNoMessagesOn(queue)
    await untilCallTo {
      queue?.sqsDlqClient?.countMessagesOnQueue(queue.dlqUrl!!)?.get()
    } matches { it == 0 }
  }

  fun expectOneMessageOnDlq(queue: HmppsQueue?) {
    expectNoMessagesOn(queue)
    await untilCallTo {
      queue?.sqsDlqClient?.countMessagesOnQueue(queue.dlqUrl!!)?.get()
    } matches { it == 1 }
  }

  fun publishDomainEvent(domainEvent: DomainEvent, eventSource: DomainEventSource? = null) {
    val messageAttributes = mutableMapOf(
      "eventType" to MessageAttributeValue.builder().dataType("String")
        .stringValue(domainEvent.eventType).build(),
    )
    if (eventSource != null) {
      messageAttributes["eventSource"] = MessageAttributeValue.builder().dataType("String")
        .stringValue(eventSource.identifier).build()
    }
    publishEvent(
      message = jsonMapper.writeValueAsString(domainEvent),
      topic = domainEventsTopic,
      messageAttributes = messageAttributes,
      eventType = domainEvent.eventType,
    )
    expectNoMessagesOn(probationEventsQueue)
    expectNoMessagesOn(probationMergeEventsQueue)
    expectNoMessagesOn(prisonMergeEventsQueue)
    expectNoMessagesOn(prisonEventsQueue)
  }

  private fun publishEvent(
    message: String,
    topic: HmppsTopic?,
    messageAttributes: Map<String, MessageAttributeValue>,
    eventType: String,
  ): String? = topic?.publish(event = message, eventType = eventType, attributes = messageAttributes)?.messageId()

  fun probationMergeEventAndResponseSetup(
    sourceCrn: String,
    targetCrn: String,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String? = null,
    nextScenarioState: String? = null,
    apiResponseSetup: ApiResponseSetup = ApiResponseSetup(crn = targetCrn),
  ) {
    stubSingleProbationResponse(apiResponseSetup, scenario, currentScenarioState, nextScenarioState)
    publishDomainEvent(
      ProbationOffenderMerged(
        eventType = OFFENDER_MERGED,
        occurredAt = Instant.now().asStringWithUkZone(),
        additionalInformation = ProbationOffenderMergedInfo(
          sourceCrn = sourceCrn,
          targetCrn = targetCrn,
        ),
      ),
    )
  }

  fun probationUnmergeEventAndResponseSetup(
    reactivatedCrn: String,
    unmergedCrn: String,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
    reactivatedSetup: ApiResponseSetup = ApiResponseSetup(crn = reactivatedCrn),
    unmergedSetup: ApiResponseSetup = ApiResponseSetup(crn = unmergedCrn),
  ) {
    stubSingleProbationResponse(reactivatedSetup, scenario, currentScenarioState, nextScenarioState)
    stubSingleProbationResponse(unmergedSetup, scenario, currentScenarioState, nextScenarioState)

    publishDomainEvent(
      ProbationOffenderUnMerged(
        eventType = OFFENDER_UNMERGED,
        occurredAt = Instant.now().asStringWithUkZone(),
        additionalInformation = ProbationOffenderUnMergedInfo(
          reactivatedCrn = reactivatedCrn,
          unmergedCrn = unmergedCrn,
        ),
      ),
    )
  }

  fun probationDomainEventAndResponseSetup(
    eventType: String,
    apiResponseSetup: ApiResponseSetup,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
  ) {
    stubSingleProbationResponse(apiResponseSetup, scenario, currentScenarioState, nextScenarioState)

    publishProbationDomainEvent(eventType, apiResponseSetup.crn!!)
  }

  fun publishProbationDomainEvent(
    eventType: String,
    crn: String,
  ) {
    val domainEvent = toProbationEvent(eventType, crn)
    publishDomainEvent(domainEvent)
  }

  fun publishPrisonDomainEvent(
    eventType: String,
    prisonNumber: String,
  ) {
    val domainEvent = toPrisonEvent(eventType, prisonNumber)
    publishDomainEvent(domainEvent)
  }

  fun prisonDomainEvent(eventType: String, prisonNumber: String) = toPrisonEvent(eventType, prisonNumber)

  fun prisonDomainEventAndResponseSetup(
    eventType: String,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
    apiResponseSetup: ApiResponseSetup,
  ) {
    stubPrisonResponse(
      apiResponseSetup,
      scenario,
      currentScenarioState,
      nextScenarioState,
    )

    publishDomainEvent(
      prisonDomainEvent(
        eventType,
        apiResponseSetup.prisonNumber!!,
      ),
    )
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

    publishDomainEvent(
      PrisonPrisonerMerged(
        eventType = PRISONER_MERGED,
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", targetPrisonNumber))),
        additionalInformation = PrisonPrisonerMergedInfo(
          sourcePrisonNumber = sourcePrisonNumber,
        ),
      ),
    )
  }

  @BeforeEach
  fun beforeEachMessagingTest() {
    purgeQueueAndDlq(courtEventsQueue)
    purgeQueueAndDlq(probationEventsQueue)
    purgeQueueAndDlq(probationMergeEventsQueue)
    purgeQueueAndDlq(probationDeleteEventsQueue)
    purgeQueueAndDlq(prisonEventsQueue)
    purgeQueueAndDlq(prisonMergeEventsQueue)
    purgeQueueAndDlq(testOnlyCourtEventsQueue)
    purgeQueueAndDlq(testOnlyCPRDomainEventsQueue)
    purgeQueueAndDlq(sasEventsQueue)
    purgeQueueAndDlq(probationAddressEventsFromCPRQueue)
  }

  fun purgeQueueAndDlq(hmppsQueue: HmppsQueue?) {
    hmppsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(hmppsQueue.queueUrl).build(),
    ).get()
    hmppsQueue.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(hmppsQueue.dlqUrl).build(),
    ).get()
  }

  private fun toProbationEvent(eventType: String, crn: String): DomainEvent = when (eventType) {
    NEW_OFFENDER_CREATED -> ProbationOffenderCreated(
      eventType = eventType,
      occurredAt = Instant.now().asStringWithUkZone(),
      personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
    )
    OFFENDER_PERSONAL_DETAILS_UPDATED -> ProbationOffenderUpdated(
      eventType = eventType,
      occurredAt = Instant.now().asStringWithUkZone(),
      personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
    )
    OFFENDER_DELETION, OFFENDER_GDPR_DELETION -> ProbationOffenderDeleted(
      eventType = eventType,
      occurredAt = Instant.now().asStringWithUkZone(),
      personReference = PersonReference(listOf(PersonIdentifier("CRN", crn))),
    )
    else -> throw IllegalArgumentException("Unknown event type '$eventType'")
  }

  private fun toPrisonEvent(eventType: String, prisonNumber: String): DomainEvent = when (eventType) {
    PRISONER_CREATED -> PrisonPrisonerCreated(
      eventType = eventType,
      occurredAt = Instant.now().asStringWithUkZone(),
      personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
    )
    PRISONER_UPDATED -> PrisonPrisonerUpdated(
      eventType = eventType,
      occurredAt = Instant.now().asStringWithUkZone(),
      personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
    )
    else -> throw IllegalArgumentException("Unknown event type '$eventType'")
  }
}
