package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.config.TransactionalLookup
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DL
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupContact
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupIdentifier
import java.lang.Thread.sleep
import java.time.LocalDate

class PrisonEventListenerIntTest(
  @Autowired private val transactionalLookup: TransactionalLookup,
  @Autowired private val pseudonymRepository: PseudonymRepository,
) : MessagingTestBase() {

  @Test
  fun `should put message on dlq when exception thrown`() {
    val prisonNumber = randomPrisonNumber()
    stub5xxResponse("/prisoner/$prisonNumber", currentScenarioState = "next request will fail", nextScenarioState = "next request will fail", scenarioName = "processing fail")
    stub5xxResponse("/prisoner/$prisonNumber", "next request will fail", scenarioName = "processing fail")
    stub5xxResponse("/prisoner/$prisonNumber", "next request will fail", scenarioName = "processing fail")
    publishPrisonPersonUpdatedEvent(prisonNumber)
    expectOneMessageOnDlq(prisonEventsQueue)
  }

  @Test
  fun `should discard message if prisoner search returns 404`() {
    val prisonNumber = randomPrisonNumber()
    stub404Response("/prisoner/$prisonNumber")
    publishPrisonPersonUpdatedEvent(prisonNumber)
    waitForMessageToBeProcessedAndDiscarded()
    expectNoMessagesOnQueueOrDlq(prisonEventsQueue)
  }

  // TODO this test can be removed when we stop listening for create events completely.
  @Test
  fun `should not create a person when prisoner created event published`() {
    val prisonNumber = randomPrisonNumber()
    stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber))
    publishDomainEvent(
      PrisonPersonCreated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
    waitForMessageToBeProcessedAndDiscarded()
    expectNoMessagesOnQueueOrDlq(prisonEventsQueue)
    assertThat(personRepository.findByPrisonNumber(prisonNumber)).isNull()
    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber), times = 0)
  }

  @Test
  fun `should try multiple times with an update message where the person does not exist yet in cpr`() {
    val prisonNumber = randomPrisonNumber()
    prisonUpdateEventAndResponseSetup(ApiResponseSetup(prisonNumber = prisonNumber))
    expectOneMessageOnDlq(prisonEventsQueue)
  }

  @Nested
  inner class AddressesAndContacts {
    @Test
    fun `should not save address and contact when saving person level data`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber).copy(addresses = mutableListOf()))

      val addressResponse = ApiResponseSetupAddress(postcode = randomPostcode())
      val contactResponse = ApiResponseSetupContact(value = randomPhoneNumber(), type = ContactType.HOME)

      stubNoMatchesPersonMatch()
      stubPersonMatchUpsert()
      prisonUpdateEventAndResponseSetup(
        ApiResponseSetup(
          prisonNumber = prisonNumber,
          firstName = randomName(),
          addresses = listOf(addressResponse),
          contacts = listOf(contactResponse),
        ),
      )

      checkTelemetry(CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
      val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(actualPersonEntity.addresses).isEmpty()
      assertThat(actualPersonEntity.contacts).isEmpty()
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
    }

    @Test
    fun `should receive the message successfully when prisoner updated event published`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val title = randomTitleCode()
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()
      val pnc = randomShortPnc()
      val email = randomEmail()
      val cro = randomCro()
      val postcode = randomPostcode()
      val fullAddress = randomFullAddress()
      val nationality = randomPrisonNationalityCode()
      val religion = randomReligionCode()
      val personDateOfBirth = randomDate()
      val nationalInsuranceNumber = randomNationalInsuranceNumber()
      val driverLicenseNumber = randomDriverLicenseNumber()
      val ethnicity = randomPrisonEthnicity()
      val sentenceStartDate = randomDate()
      val primarySentence = true
      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()
      val aliasGender = randomPrisonSexCode()
      val gender = randomPrisonSexCode()

      stubNoMatchesPersonMatch()
      prisonUpdateEventAndResponseSetup(
        ApiResponseSetup(
          title = title.key,
          gender = gender.key,
          aliases = listOf(ApiResponseSetupAlias(title.key, aliasFirstName, aliasMiddleName, aliasLastName, aliasDateOfBirth, aliasGender.key)),
          firstName = firstName,
          middleName = middleName,
          lastName = lastName,
          prisonNumber = prisonNumber,
          pnc = pnc,
          email = email,
          sentenceStartDate = sentenceStartDate,
          primarySentence = primarySentence,
          cro = cro,
          addresses = listOf(ApiResponseSetupAddress(postcode = postcode, fullAddress = fullAddress, startDate = LocalDate.of(1970, 1, 1), noFixedAbode = true)),
          dateOfBirth = personDateOfBirth,
          nationality = nationality,
          ethnicity = ethnicity,
          religion = religion.name,
          contacts = listOf(ApiResponseSetupContact(type = ContactType.MOBILE, value = "01141234567"), ApiResponseSetupContact(type = ContactType.HOME, value = "01141234568")),
          identifiers = listOf(ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber), ApiResponseSetupIdentifier(type = "DL", value = driverLicenseNumber)),
        ),
      )

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.personKey).isNotNull()
        assertThat(personEntity.personKey?.status).isEqualTo(UUIDStatusType.ACTIVE)
        assertThat(personEntity.getPrimaryName().titleCode).isEqualTo(title.value)
        assertThat(personEntity.getPrimaryName().firstName).isEqualTo(firstName)
        assertThat(personEntity.getPrimaryName().middleNames).isEqualTo("$middleName $middleName")
        assertThat(personEntity.getPrimaryName().lastName).isEqualTo(lastName)
        assertThat(personEntity.getPrimaryName().sexCode).isEqualTo(gender.value)
        assertThat(personEntity.getPrimaryName().ethnicityCode).isEqualTo(EthnicityCode.fromPrison(ethnicity))
        assertThat(personEntity.religion).isNull()

        val populatedReferencesUpdateIdCount = personEntity.references.count { it.updateId != null }
        assertThat(populatedReferencesUpdateIdCount).isEqualTo(4)
        assertThat(personEntity.getPnc()).isEqualTo(PNCIdentifier.from(pnc).pncId)
        assertThat(personEntity.getCro()).isEqualTo(cro)
        assertThat(personEntity.references.getType(NATIONAL_INSURANCE_NUMBER).first()).isEqualTo(nationalInsuranceNumber)
        assertThat(personEntity.references.getType(DL).first()).isEqualTo(driverLicenseNumber)

        assertThat(personEntity.getPrimaryName().dateOfBirth).isEqualTo(personDateOfBirth)

        assertThat(personEntity.getAliases().size).isEqualTo(1)
        assertThat(personEntity.getAliases()[0].titleCode).isEqualTo(title.value)
        assertThat(personEntity.getAliases()[0].firstName).isEqualTo(aliasFirstName)
        assertThat(personEntity.getAliases()[0].middleNames).isEqualTo(aliasMiddleName)
        assertThat(personEntity.getAliases()[0].lastName).isEqualTo(aliasLastName)
        assertThat(personEntity.getAliases()[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
        assertThat(personEntity.getAliases()[0].sexCode).isEqualTo(aliasGender.value)
        assertThat(personEntity.getAliases()[0].ethnicityCode).isNull()
        assertThat(personEntity.pseudonyms.size).isEqualTo(2)
        val populatedPseudonymsUpdateIdCount = personEntity.pseudonyms.count { it.updateId != null }
        assertThat(populatedPseudonymsUpdateIdCount).isEqualTo(2)

        assertThat(personEntity.sentenceInfo[0].sentenceDate).isEqualTo(sentenceStartDate)
        assertThat(personEntity.nationalities.size).isEqualTo(1)
        assertThat(personEntity.nationalities.first().nationalityCode.name).isEqualTo(NationalityCode.fromPrisonMapping(nationality)?.name)
        assertThat(personEntity.nationalities.first().nationalityCode.description).isEqualTo(NationalityCode.fromPrisonMapping(nationality)?.description)
        assertThat(personEntity.ethnicityCode).isEqualTo(EthnicityCode.fromPrison(ethnicity))
      }

      checkEventLogExist(prisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `should check sentence start date is not populated when primary sentence is false`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val sentenceStartDate = randomDate()

      stubNoMatchesPersonMatch()
      prisonUpdateEventAndResponseSetup(ApiResponseSetup(prisonNumber = prisonNumber, sentenceStartDate = sentenceStartDate, primarySentence = false))

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(personEntity.sentenceInfo.size).isEqualTo(0)
      }
    }

    @Test
    fun `should update prisoner when an existing reference is linked to a pseudonym`() {
      val prisonNumber = randomPrisonNumber()
      val nationalInsuranceNumber = randomNationalInsuranceNumber()

      // Create a person with a reference that will be reused.
      // It is attached to a pseudonym.
      // The pseudonym will be removed in the update.
      val person = createPersonWithNewKey(
        createRandomPrisonPersonDetails(prisonNumber)
          .copy(
            references = listOf(
              Reference(NATIONAL_INSURANCE_NUMBER, nationalInsuranceNumber),
            ),
          ),
        configure = {
          val primaryName = getPrimaryName()
          references.forEach { reference ->
            reference.pseudonym = primaryName
            primaryName.references.add(reference)
          }
        },
      )

      val savedPerson = transactionalLookup.findByPrisonNumberWithReferences(person.prisonNumber!!)
      val referenceToBeReused = savedPerson.references.single { it.identifierType == NATIONAL_INSURANCE_NUMBER }
      val pseudonymLinkedToReferenceToBeRemoved = referenceToBeReused.pseudonym
      assertThat(referenceToBeReused.pseudonym).isNotNull()
      assertThat(pseudonymLinkedToReferenceToBeRemoved).isNotNull()

      stubNoMatchesPersonMatch()
      prisonUpdateEventAndResponseSetup(
        ApiResponseSetup(
          prisonNumber = prisonNumber,
          firstName = randomName(),
          identifiers = listOf(ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber)),
        ),
      )

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      expectNoMessagesOnQueueOrDlq(prisonEventsQueue)

      awaitAssert {
        val personUpdated = transactionalLookup.findByPrisonNumberWithReferences(person.prisonNumber!!)
        assertThat(personUpdated.references.single { it.identifierType == NATIONAL_INSURANCE_NUMBER }.id).isEqualTo(referenceToBeReused.id)
        assertThat(pseudonymRepository.findByIdOrNull(pseudonymLinkedToReferenceToBeRemoved!!.id!!)).isNull()
      }
    }

    @Test
    fun `should retry on retryable 500 error from prisoner search`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      stubNoMatchesPersonMatch()
      stub5xxResponse("/prisoner/$prisonNumber", nextScenarioState = "next request will succeed", scenarioName = "retry")
      stubPrisonResponse(ApiResponseSetup(prisonNumber = prisonNumber), scenarioName = "retry", currentScenarioState = "next request will succeed")

      publishPrisonPersonUpdatedEvent(prisonNumber)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
    }

    @Test
    fun `should retry message if person-match returns 404`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      stubPrisonResponse(ApiResponseSetup(gender = "Male", prisonNumber = prisonNumber))
      stubPersonMatchScores(status = 404, nextScenarioState = "will succeed")

      stubPrisonResponse(ApiResponseSetup(gender = "Male", prisonNumber = prisonNumber), currentScenarioState = "will succeed")
      stubPersonMatchUpsert(currentScenarioState = "will succeed")
      stubPersonMatchScores(currentScenarioState = "will succeed")
      publishPrisonPersonUpdatedEvent(prisonNumber)

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber),
      )
      expectNoMessagesOnQueueOrDlq(prisonEventsQueue)
    }
  }

  @Nested
  inner class EventLog {

    @Test
    fun `should save record details in event log on update`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()
      val pnc = randomShortPnc()
      val cro = randomCro()
      val postcode = randomPostcode()
      val personDateOfBirth = randomDate()
      val sentenceStartDate = randomDate()
      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()
      val secondAliasFirstName = randomName()
      val secondAliasMiddleName = randomName()
      val secondAliasLastName = randomName()
      val secondAliasDateOfBirth = randomDate()

      stubPersonMatchUpsert()
      stubNoMatchesPersonMatch()
      prisonUpdateEventAndResponseSetup(
        ApiResponseSetup(
          aliases = listOf(ApiResponseSetupAlias(firstName = aliasFirstName, middleName = aliasMiddleName, lastName = aliasLastName, dateOfBirth = aliasDateOfBirth), ApiResponseSetupAlias(firstName = secondAliasFirstName, middleName = secondAliasMiddleName, lastName = secondAliasLastName, dateOfBirth = secondAliasDateOfBirth)), firstName = firstName, middleName = middleName, lastName = lastName, prisonNumber = prisonNumber, pnc = pnc, sentenceStartDate = sentenceStartDate, primarySentence = true, cro = cro,
          addresses = listOf(
            ApiResponseSetupAddress(
              postcode = postcode,
              startDate = LocalDate.of(1970, 1, 1),
              noFixedAbode = true,
              fullAddress = "",
            ),
          ),
          dateOfBirth = personDateOfBirth,
        ),
      )

      checkEventLog(prisonNumber, CPRLogEvents.CPR_RECORD_UPDATED) { eventLogs ->
        assertThat(eventLogs.size).isEqualTo(1)
        val createdLog = eventLogs.first()
        assertThat(createdLog.pncs).isEqualTo(listOf(PNCIdentifier.from(pnc).pncId))
        assertThat(createdLog.cros).isEqualTo(listOf(cro))
        assertThat(createdLog.firstName).isEqualTo(firstName)
        assertThat(createdLog.middleNames).isEqualTo("$middleName $middleName")
        assertThat(createdLog.lastName).isEqualTo(lastName)
        assertThat(createdLog.dateOfBirth).isEqualTo(personDateOfBirth)
        assertThat(createdLog.sourceSystem).isEqualTo(NOMIS)
        assertThat(createdLog.sentenceDates).isEqualTo(listOf(sentenceStartDate))
        assertThat(createdLog.firstNameAliases).isEqualTo(listOf(aliasFirstName, secondAliasFirstName).sorted())
        assertThat(createdLog.lastNameAliases).isEqualTo(listOf(aliasLastName, secondAliasLastName).sorted())
        assertThat(createdLog.dateOfBirthAliases).isEqualTo(listOf(aliasDateOfBirth, secondAliasDateOfBirth).sorted())
        assertThat(createdLog.personUUID).isNotNull()
        assertThat(createdLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
      }
    }
  }

  private fun waitForMessageToBeProcessedAndDiscarded() {
    sleep(500)
  }

  private fun publishPrisonPersonUpdatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPersonUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }

  private fun prisonUpdateEventAndResponseSetup(apiResponseSetup: ApiResponseSetup) {
    stubPrisonResponse(apiResponseSetup)

    publishPrisonPersonUpdatedEvent(apiResponseSetup.prisonNumber!!)
  }
}
