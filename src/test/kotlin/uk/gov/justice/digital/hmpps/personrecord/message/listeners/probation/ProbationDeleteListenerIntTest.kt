package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_GDPR_DELETION
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCRN
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class ProbationDeleteListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should process offender gdpr deletion`() {
    val crn = randomCRN()
    val domainEvent = buildDomainEvent(crn)
    val personKey = createPersonKey()
    createPerson(
      Person.from(ProbationCase(name = Name(firstName = randomName(), lastName = randomName()), identifiers = Identifiers(crn = crn))),
      personKeyEntity = personKey,
    )
    publishDomainEvent(OFFENDER_GDPR_DELETION, domainEvent)

    checkTelemetry(
      MESSAGE_RECEIVED,
      mapOf("CRN" to crn, "EVENT_TYPE" to OFFENDER_GDPR_DELETION, "SOURCE_SYSTEM" to "DELIUS"),
    )
    checkTelemetry(
      CPR_RECORD_DELETED,
      mapOf("CRN" to crn, "UUID" to personKey.personId.toString(), "SOURCE_SYSTEM" to "DELIUS"),
    )
  }

  private fun buildDomainEvent(crn: String, eventType: String = OFFENDER_GDPR_DELETION): DomainEvent {
    val crnType = PersonIdentifier("CRN", crn)
    val personReference = PersonReference(listOf(crnType))

    return DomainEvent(
      eventType = eventType,
      personReference = personReference,
      additionalInformation = null,
    )
  }
}
