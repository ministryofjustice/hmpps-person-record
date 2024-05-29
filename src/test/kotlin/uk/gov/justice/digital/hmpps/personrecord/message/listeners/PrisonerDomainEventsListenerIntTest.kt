package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.integration.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.prisonerSearchResponse
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

class PrisonerDomainEventsListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    // Given
    val prisonNumber = UUID.randomUUID().toString()
    stubPrisonerResponse(prisonNumber)

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))

    await.atMost(15, SECONDS) untilAsserted {
      val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(personEntity.title).isEqualTo("Ms")
      assertThat(personEntity.firstName).isEqualTo("Robert")
      assertThat(personEntity.middleNames).isEqualTo("John James")
      assertThat(personEntity.lastName).isEqualTo("Larsen")
      assertThat(personEntity.pnc).isEqualTo(PNCIdentifier.from("2003/0062845E"))
      assertThat(personEntity.cro).isEqualTo(CROIdentifier.from("029906/12J"))
      assertThat(personEntity.aliases.size).isEqualTo(1)
      assertThat(personEntity.aliases[0].firstName).isEqualTo("Robert")
      assertThat(personEntity.aliases[0].middleNames).isEqualTo("Trevor")
      assertThat(personEntity.aliases[0].lastName).isEqualTo("Lorsen")
      assertThat(personEntity.aliases[0].dateOfBirth).isEqualTo(LocalDate.of(1975, 4, 2))
      assertThat(personEntity.addresses.size).isEqualTo(1)
      assertThat(personEntity.addresses[0].postcode).isEqualTo("S10 1BP")
      assertThat(personEntity.contacts.size).isEqualTo(3)
      assertThat(personEntity.contacts[0].contactType).isEqualTo(ContactType.EMAIL)
      assertThat(personEntity.contacts[0].contactValue).isEqualTo("john.smith@gmail.com")
      assertThat(personEntity.contacts[1].contactType).isEqualTo(ContactType.HOME)
      assertThat(personEntity.contacts[1].contactValue).isEqualTo("01141234567")
      assertThat(personEntity.contacts[2].contactType).isEqualTo(ContactType.MOBILE)
      assertThat(personEntity.contacts[2].contactValue).isEqualTo("01141234567")
    }

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    // Given
    val prisonNumber = UUID.randomUUID().toString()
    personRepository.saveAndFlush(
      PersonEntity.from(
        Person.from(
          Prisoner(
            prisonNumber = prisonNumber,
            title = "Ms",
            firstName = "Robert",
            middleNames = "John James",
            lastName = "Larsen",
            cro = CROIdentifier.from("029906/12J"),
            pnc = PNCIdentifier.from("2003/0062845E"),
            dateOfBirth = LocalDate.of(1975, 4, 2),
          ),
        ),
      ),
    )

    await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByPrisonNumber(prisonNumber)
    }

    stubPrisonerResponse(prisonNumber)

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to "NOMIS"))

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  fun stubPrisonerResponse(prisonNumber: String) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$prisonNumber")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(prisonerSearchResponse(prisonNumber)),
        ),
    )
  }
}
