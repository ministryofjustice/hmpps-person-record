package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.EmailAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonIdentifierEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.DOMAIN_EVENT_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import uk.gov.justice.digital.hmpps.personrecord.test.randomLastName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonerSearchResponse
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS

class PrisonEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    val prisonNumber = randomPrisonNumber()
    val pnc = randomPnc()
    val email = randomEmail()
    val cro = randomCro()
    val postcode = randomPostcode()
    val prefix = randomFirstName()

    stubPrisonResponse(prisonNumber = prisonNumber, pnc = pnc, email = email, cro = cro, postcode = postcode, prefix = prefix)

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))

    await.atMost(5, SECONDS) untilAsserted {
      val personEntity = personRepository.findByPrisonNumberAndSourceSystem(prisonNumber)!!
      assertThat(personEntity.personIdentifier).isNull()
      assertThat(personEntity.title).isEqualTo("Ms")
      assertThat(personEntity.firstName).isEqualTo(prefix + "FirstName")
      assertThat(personEntity.middleNames).isEqualTo(prefix + "MiddleName1 " + prefix + "MiddleName2")
      assertThat(personEntity.lastName).isEqualTo(prefix + "LastName")
      assertThat(personEntity.pnc).isEqualTo(PNCIdentifier.from(pnc))
      assertThat(personEntity.cro).isEqualTo(CROIdentifier.from(cro))
      assertThat(personEntity.aliases.size).isEqualTo(1)
      assertThat(personEntity.aliases[0].firstName).isEqualTo(prefix + "AliasFirstName")
      assertThat(personEntity.aliases[0].middleNames).isEqualTo(prefix + "AliasMiddleName")
      assertThat(personEntity.aliases[0].lastName).isEqualTo(prefix + "AliasLastName")

      assertThat(personEntity.aliases[0].dateOfBirth).isEqualTo(LocalDate.of(1975, 4, 2))
      assertThat(personEntity.addresses.size).isEqualTo(1)
      assertThat(personEntity.addresses[0].postcode).isEqualTo(postcode)
      assertThat(personEntity.contacts.size).isEqualTo(3)
      assertThat(personEntity.contacts[0].contactType).isEqualTo(EMAIL)
      assertThat(personEntity.contacts[0].contactValue).isEqualTo(email)
      assertThat(personEntity.contacts[1].contactType).isEqualTo(HOME)
      assertThat(personEntity.contacts[1].contactValue).isEqualTo("01141234567")
      assertThat(personEntity.contacts[2].contactType).isEqualTo(MOBILE)
      assertThat(personEntity.contacts[2].contactValue).isEqualTo("01141234567")
    }

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should log correct telemetry on created event but record already exists`() {
    val prisonNumber = createPrisoner()

    stubPrisonResponse(prisonNumber)

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))
    checkTelemetry(CPR_NEW_RECORD_EXISTS, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should receive the message successfully when prisoner updated event published`() {
    val prisonNumber = createPrisoner()

    await.atMost(30, SECONDS) untilNotNull {
      personRepository.findByPrisonNumberAndSourceSystem(prisonNumber)
    }

    stubPrisonResponse(prisonNumber)

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to "NOMIS"))

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should retry on retryable error`() {
    val prisonNumber = randomPrisonNumber()
    stub500Response(prisonNumber)
    stubPrisonResponse(prisonNumber, scenarioName = "retry", currentScenarioState = "next request will succeed")
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should log correct telemetry on updated event but no record exists`() {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(prisonNumber, cro = randomCro())

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(DOMAIN_EVENT_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to "NOMIS"))
    checkTelemetry(CPR_UPDATE_RECORD_DOES_NOT_EXIST, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should allow a person to be created from a prison event when an offender record already exists with the prisonNumber`() {
    val prisonNumber = randomPrisonNumber()
    probationDomainEventAndResponseSetup(eventType = OFFENDER_ALIAS_CHANGED, pnc = "", prisonNumber = prisonNumber)
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, detailUrl = createNomsDetailUrl(prisonNumber), personReference = null, additionalInformation = additionalInformation)

    stubPrisonResponse(prisonNumber)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  private fun createPrisoner(): String {
    val prisonNumber = randomPrisonNumber()

    val person = PersonEntity.from(
      Person.from(
        Prisoner(
          prisonNumber = prisonNumber,
          title = "Ms",
          firstName = randomFirstName(),
          middleNames = "John James",
          lastName = randomLastName(),
          cro = CROIdentifier.from(randomCro()),
          pnc = PNCIdentifier.from(randomPnc()),
          dateOfBirth = LocalDate.of(1975, 4, 2),
          emailAddresses = listOf(EmailAddress(randomEmail())),

        ),
      ),
    )
    val personIdentifier = PersonIdentifierEntity.new()

    personIdentifierRepository.saveAndFlush(personIdentifier)
    person.personIdentifier = personIdentifier
    personRepository.saveAndFlush(
      person,
    )
    return prisonNumber
  }

  private fun stubPrisonResponse(
    prisonNumber: String,
    pnc: String? = randomPnc(),
    email: String? = randomEmail(),
    cro: String? = randomCro(),
    postcode: String = randomPostcode(),
    prefix: String? = randomFirstName(),
    scenarioName: String? = "scenario",
    currentScenarioState: String? = STARTED,
  ) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$prisonNumber")
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(prisonerSearchResponse(ApiResponseSetup(prisonNumber = prisonNumber, pnc = pnc, email = email, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode)), prefix = prefix))),
        ),
    )
  }

  private fun stub500Response(
    prisonNumber: String,
  ) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$prisonNumber")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo("next request will succeed")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }
}
