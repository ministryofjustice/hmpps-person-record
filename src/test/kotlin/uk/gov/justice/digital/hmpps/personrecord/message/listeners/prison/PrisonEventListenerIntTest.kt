package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.EmailAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateOfBirth
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.prisonerSearchResponse
import java.util.concurrent.TimeUnit.SECONDS

class PrisonEventListenerIntTest : MessagingMultiNodeTestBase() {

  @Test
  fun `should receive the message successfully when prisoner created event published`() {
    val prisonNumber = randomPrisonNumber()
    val pnc = randomPnc()
    val email = randomEmail()
    val cro = randomCro()
    val postcode = randomPostcode()
    val prefix = randomName()
    val personDateOfBirth = randomDateOfBirth()

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber, pnc = pnc, email = email, cro = cro, addresses = listOf(ApiResponseSetupAddress(postcode)), prefix = prefix, dateOfBirth = personDateOfBirth))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))

    await.atMost(5, SECONDS) untilAsserted {
      val personEntity = personRepository.findByPrisonNumberAndSourceSystem(prisonNumber)!!
      assertThat(personEntity.personKey).isNotNull()
      assertThat(personEntity.title).isEqualTo("Ms")
      assertThat(personEntity.firstName).isEqualTo(prefix + "FirstName")
      assertThat(personEntity.middleNames).isEqualTo(prefix + "MiddleName1 " + prefix + "MiddleName2")
      assertThat(personEntity.lastName).isEqualTo(prefix + "LastName")
      assertThat(personEntity.references.getType(IdentifierType.PNC).first().identifierValue).isEqualTo(pnc)
      assertThat(personEntity.references.getType(IdentifierType.CRO).first().identifierValue).isEqualTo(cro)
      assertThat(personEntity.dateOfBirth).isEqualTo(personDateOfBirth)
      assertThat(personEntity.pseudonyms.size).isEqualTo(1)
      assertThat(personEntity.pseudonyms[0].firstName).isEqualTo(prefix + "AliasFirstName")
      assertThat(personEntity.pseudonyms[0].middleNames).isEqualTo(prefix + "AliasMiddleName")
      assertThat(personEntity.pseudonyms[0].lastName).isEqualTo(prefix + "AliasLastName")

      assertThat(personEntity.pseudonyms[0].dateOfBirth).isEqualTo(personDateOfBirth)
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
    checkTelemetry(CPR_UUID_CREATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
  }

  @Test
  fun `should log correct telemetry on created event but record already exists`() {
    val prisonNumber = createPrisoner()

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))
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

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to "NOMIS"))

    checkTelemetry(
      CPR_RECORD_UPDATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should retry on retryable error`() {
    val prisonNumber = randomPrisonNumber()
    stub500Response(prisonNumber, "next request will succeed")
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber), scenarioName = "retry", currentScenarioState = "next request will succeed")
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_CREATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
    checkTelemetry(
      CPR_UUID_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should create message processing failed telemetry event when exception thrown`() {
    val prisonNumber = randomPrisonNumber()
    stub500Response(prisonNumber, STARTED)
    stub500Response(prisonNumber, STARTED)
    stub500Response(prisonNumber, STARTED)
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = listOf("SENTENCE"))
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)
    val messageId = publishDomainEvent(PRISONER_CREATED, domainEvent)

    prisonEventsQueue!!.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonEventsQueue!!.queueUrl).build(),
    ).get()
    prisonEventsQueue!!.sqsDlqClient!!.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(prisonEventsQueue!!.dlqUrl).build(),
    ).get()
    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_CREATED, "SOURCE_SYSTEM" to "NOMIS"))

    checkTelemetry(
      TelemetryEventType.MESSAGE_PROCESSING_FAILED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to "NOMIS",
        "EVENT_TYPE" to "prisoner-offender-search.prisoner.created",
      ),
    )
  }

  @Test
  fun `should log correct telemetry on updated event but no record exists`() {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))

    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_UPDATED, personReference = null, additionalInformation = additionalInformation)
    publishDomainEvent(PRISONER_UPDATED, domainEvent)

    checkTelemetry(MESSAGE_RECEIVED, mapOf("PRISON_NUMBER" to prisonNumber, "EVENT_TYPE" to PRISONER_UPDATED, "SOURCE_SYSTEM" to "NOMIS"))
    checkTelemetry(CPR_UPDATE_RECORD_DOES_NOT_EXIST, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
    checkTelemetry(
      CPR_UUID_CREATED,
      mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber),
    )
  }

  @Test
  fun `should allow a person to be created from a prison event when an offender record already exists with the prisonNumber`() {
    val prisonNumber = randomPrisonNumber()
    probationDomainEventAndResponseSetup(eventType = OFFENDER_ALIAS_CHANGED, pnc = "", prisonNumber = prisonNumber)
    val additionalInformation = AdditionalInformation(prisonNumber = prisonNumber, categoriesChanged = emptyList())
    val domainEvent = DomainEvent(eventType = PRISONER_CREATED, personReference = null, additionalInformation = additionalInformation)

    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
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
          firstName = randomName(),
          middleNames = "${randomName()} ${randomName()}",
          lastName = randomName(),
          cro = CROIdentifier.from(randomCro()),
          pnc = PNCIdentifier.from(randomPnc()),
          dateOfBirth = randomDateOfBirth(),
          emailAddresses = listOf(EmailAddress(randomEmail())),

        ),
      ),
    )
    val personKey = PersonKeyEntity.new()

    personKeyRepository.saveAndFlush(personKey)
    person.personKey = personKey
    personRepository.saveAndFlush(
      person,
    )
    return prisonNumber
  }

  private fun stubPrisonResponse(
    apiResponseSetup: ApiResponseSetup,
    scenarioName: String? = "scenario",
    currentScenarioState: String? = STARTED,
  ) {
    wiremock.stubFor(
      WireMock.get("/prisoner/${apiResponseSetup.prisonNumber}")
        .inScenario(scenarioName)
        .whenScenarioStateIs(currentScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(prisonerSearchResponse(apiResponseSetup)),
        ),
    )
  }

  private fun stub500Response(
    prisonNumber: String,
    nextScenarioState: String,
  ) {
    wiremock.stubFor(
      WireMock.get("/prisoner/$prisonNumber")
        .inScenario("retry")
        .whenScenarioStateIs(STARTED)
        .willSetStateTo(nextScenarioState)
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }
}
