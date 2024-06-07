package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingSingleNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.UUID

@ActiveProfiles("seeding")
@Disabled("passes in isolation but not with other tests")
class OffenderDomainEventsListenerDisabledIntTest : MessagingSingleNodeTestBase() {

  @Test
  fun `should not process any messages`() {
    val prisonNumber = UUID.randomUUID().toString()
    val crn = domainEvent(NEW_OFFENDER_CREATED, "2020/0476873U", prisonNumber = prisonNumber)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("CRN" to crn, "EVENT_TYPE" to NEW_OFFENDER_CREATED, "SOURCE_SYSTEM" to "DELIUS"), 0)
  }

  private fun domainEvent(eventType: String, crn: String = UUID.randomUUID().toString(), additionalInformation: AdditionalInformation? = null, prisonNumber: String = UUID.randomUUID().toString()): String {
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    val domainEvent = DomainEvent(
      eventType = eventType,
      detailUrl = createDeliusDetailUrl(crn),
      personReference = personReference,
      additionalInformation = additionalInformation,
    )
    publishDomainEvent(eventType, domainEvent)
    await untilCallTo {
      offenderEventsQueue?.sqsClient?.countMessagesOnQueue(offenderEventsQueue!!.queueUrl)?.get()
    } matches { it == 1 }
    return crn
  }
}
