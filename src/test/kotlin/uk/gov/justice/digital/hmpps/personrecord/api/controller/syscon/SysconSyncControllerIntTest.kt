package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AddressContactMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AliasMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.IdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.PersonContactMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconUpdatePersonResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.SentenceInfoRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Address as SysconAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.AddressUsage as SysconAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Alias as SysconAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Contact as SysconContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.DemographicAttributes as SysconDemographicAttributes
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Identifier as SysconIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.IdentifierType as SysconIdentifierType
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Sentence as SysconSentence

class SysconSyncControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var sentenceInfoRepository: SentenceInfoRepository

  @Nested
  inner class SuccessfulUpdate {
    @Test
    fun `updates person record & overwrites child tables`() {
      val prisonNumber = randomPrisonNumber()
      createRandomPrisonPerson(prisonNumber)

      val updatePrisonerRequestBody = buildRequestBody()
      sendPutRequestAsserted<SysconUpdatePersonResponse>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatePrisonerRequestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      assertDatabase(prisonNumber, updatePrisonerRequestBody)
    }

    @Test
    fun `returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      createRandomPrisonPerson(prisonNumber)

      val updatePrisonerRequestBody = buildRequestBody()
      val responseBody = sendPutRequestAsserted<SysconUpdatePersonResponse>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatePrisonerRequestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("Expected to find Person")

      val personAddressEntity = personEntity.addresses.first()
      val addressUsageEntityUpdateId = personAddressEntity.usages.first().updateId.toString()
      val addressContactEntityUpdateId = personAddressEntity.contacts.first().updateId.toString()

      val personContactEntityUpdateId = personEntity.contacts.first().updateId.toString()

      val pseudonymEntityUpdateId = personEntity.pseudonyms.first().updateId.toString()

      val personReferenceEntityUpdateId = personEntity.references.first().updateId.toString()

      val expectedAddressNomisId = updatePrisonerRequestBody.addresses.first().nomisAddressId.toString()
      val expectedAddressUsageNomisId = updatePrisonerRequestBody.addresses.first().addressUsage.first().nomisAddressUsageId.toString()
      val expectedAddressContactNomisId = updatePrisonerRequestBody.addresses.first().contacts.first().nomisContactId.toString()
      val expectedPersonContactNomisId = updatePrisonerRequestBody.personContacts.first().nomisContactId.toString()
      val expectedPseudonymNomisId = updatePrisonerRequestBody.aliases.first().nomisAliasId.toString()
      val expectedReferenceNomisId = updatePrisonerRequestBody.aliases.first().identifiers.first().nomisIdentifierId.toString()

      val expectedResponseBody = SysconUpdatePersonResponse(
        prisonerId = prisonNumber,
        addressMappings = listOf(
          AddressMapping(
            nomisAddressId = expectedAddressNomisId,
            cprAddressId = personAddressEntity.updateId.toString(),
            addressUsageMappings = listOf(
              AddressUsageMapping(
                nomisAddressUsageId = expectedAddressUsageNomisId,
                cprAddressUsageid = addressUsageEntityUpdateId,
              ),
            ),
            addressContactMappings = listOf(
              AddressContactMapping(
                nomisContactId = expectedAddressContactNomisId,
                cprContactId = addressContactEntityUpdateId,
              ),
            ),
          ),
        ),
        personContactMappings = listOf(
          PersonContactMapping(
            nomisContactId = expectedPersonContactNomisId,
            cprContactId = personContactEntityUpdateId,
          ),
        ),
        pseudonymMappings = listOf(
          AliasMapping(
            nomisPseudonymId = expectedPseudonymNomisId,
            cprPseudonymId = pseudonymEntityUpdateId,
            identifierMappings = listOf(
              IdentifierMapping(
                nomisIdentifierId = expectedReferenceNomisId,
                cprIdentifierId = personReferenceEntityUpdateId,
              ),
            ),
          ),
        ),
      )

      responseBody.isEqualTo(expectedResponseBody)
    }

    @Test
    fun `sending 2 syscon alias - one is primary & one is not - saves two pseudonyms records - returns two alias mappings`() {
      val prisonNumber = randomPrisonNumber()
      createRandomPrisonPerson(prisonNumber)

      val request = buildRequestBody()
      val aliasList = request.aliases + SysconAlias(
        nomisAliasId = randomCId().toLong(),
        titleCode = randomTitleCode().value.name,
        firstName = randomName(),
        middleNames = randomName(),
        lastName = randomName(),
        dateOfBirth = LocalDate.now().minusYears((30..70).random().toLong()),
        sexCode = SexCode.entries.random(),
        isPrimary = false,
        identifiers = listOf(
          SysconIdentifier(
            nomisIdentifierId = randomCId().toLong(),
            type = SysconIdentifierType.PNC,
            value = randomName(),
            comment = randomName(),
          ),
        ),
      )
      val updatePrisonerRequestBody = request.copy(aliases = aliasList)

      val responseBody = sendPutRequestAsserted<SysconUpdatePersonResponse>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatePrisonerRequestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      assertThat(responseBody.pseudonymMappings.size).isEqualTo(2)

      val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail { "Prisoner record was expected to be found" }
      assertThat(actualPersonEntity.pseudonyms.size).isEqualTo(2)
    }
  }

  @Nested
  inner class BadRequest {
    @Test
    fun `person record does not exists - does not insert - returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      val updatedPrisonerRequest = buildRequestBody()

      sendPutRequestAsserted<Unit>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatedPrisonerRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      assertDatabase(prisonNumber, updatedPrisonerRequest, isWriteExpected = false)
    }

    @Test // TODO: do we really want this?!?
    fun `no primary alias is sent - does not update - returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val updatedPrisonerRequest = buildRequestBody().copy(aliases = buildAliasList(false))
      val responseBody = sendPutRequestAsserted<ErrorResponse>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatedPrisonerRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.BAD_REQUEST,
      )
      responseBody.isEqualTo(ErrorResponse(status = 400, userMessage = "Bad request: No primary alias was found for update on prisoner $prisonNumber"))

      val actualPerson = personRepository.findByPrisonNumber(prisonNumber)?.let { Person.from(it) } ?: fail { "Person not found for update on prisoner $prisonNumber" }
      val expectedPerson = Person.from(originalPerson)
      assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
    }
  }

  @Nested
  inner class Auth {
    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val prisonNumber = randomPrisonNumber()
      val updatedPrisonerRequest = buildRequestBody()

      sendPutRequestAsserted<Unit>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatedPrisonerRequest,
        roles = listOf("UNSUPPORTED-ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )

      assertDatabase(prisonNumber, updatedPrisonerRequest, isWriteExpected = false)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val prisonNumber = randomPrisonNumber()
      val updatedPrisonerRequest = buildRequestBody()
      sendPutRequestAsserted<Unit>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatedPrisonerRequest,
        roles = emptyList(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )

      assertDatabase(prisonNumber, updatedPrisonerRequest, isWriteExpected = false)
    }
  }

  fun createRandomPrisonPerson(prisonNumber: String): Person {
    val person = createRandomPrisonPersonDetails(prisonNumber).copy(
      contacts = listOf(
        Contact(
          contactType = ContactType.entries.random(),
          contactValue = randomLowerCaseString(),
        ),
      ),
      references = listOf(
        Reference(
          identifierType = IdentifierType.entries.random(),
          identifierValue = randomLowerCaseString(),
          comment = randomLowerCaseString(),
        ),
      ),
    )
    createPerson(person)
    return person
  }

  private fun assertDatabase(prisonNumber: String, updatedPrisonerRequest: Prisoner, isWriteExpected: Boolean = true) {
    if (isWriteExpected) {
      val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail { "Prisoner record was expected to be found" }

      val actualPerson = Person.from(actualPersonEntity)
      val expectedPerson = Person.from(updatedPrisonerRequest, prisonNumber).copy(
        personId = actualPerson.personId,
        aliases = updatedPrisonerRequest.aliases.filter { it.isPrimary == false }.map { Alias.from(it) },
      )
      assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
    } else {
      assertThat(personRepository.findByPrisonNumber(prisonNumber)).isNull()
    }
  }

  companion object {
    fun buildRequestBody(): Prisoner = Prisoner(
      demographicAttributes = SysconDemographicAttributes(
        birthPlace = randomName(),
        birthCountryCode = randomCountryCode(),
        ethnicityCode = randomPrisonEthnicity(),
        sexCode = randomPrisonSexCode().value,
        sexualOrientation = randomPrisonSexualOrientation().value.name,
        disability = randomBoolean(),
        interestToImmigration = randomBoolean(),
        religionCode = randomReligionCode(),
        nationalityCode = randomNationalityCode().name,
        nationalityNote = randomName(),
      ),
      aliases = buildAliasList(),
      addresses = listOf(
        SysconAddress(
          nomisAddressId = randomCId().toLong(),
          fullAddress = randomFullAddress(),
          noFixedAbode = randomBoolean(),
          startDate = LocalDate.now().minusYears((1..25).random().toLong()),
          endDate = LocalDate.now().plusYears((1..25).random().toLong()),
          postcode = randomPostcode(),
          subBuildingName = randomName(),
          buildingName = randomName(),
          buildingNumber = randomName(),
          thoroughfareName = randomName(),
          dependentLocality = randomName(),
          postTown = randomName(),
          county = randomName(),
          countryCode = CountryCode.entries.random().name,
          comment = randomName(),
          isPrimary = randomBoolean(),
          isMail = randomBoolean(),
          addressUsage = listOf(
            SysconAddressUsage(
              nomisAddressUsageId = randomCId().toLong(),
              addressUsageCode = AddressUsageCode.HOME,
              isActive = true,
            ),
          ),
          contacts = listOf(
            SysconContact(
              nomisContactId = randomCId().toLong(),
              value = randomPhoneNumber(),
              type = ContactType.entries.random(),
              extension = null,
            ),
          ),
        ),
      ),
      personContacts = listOf(
        SysconContact(
          nomisContactId = randomCId().toLong(),
          value = randomPhoneNumber(),
          type = ContactType.entries.random(),
          extension = null,
        ),
      ),
      sentences = listOf(
        SysconSentence(
          sentenceDate = randomDate(),
        ),
      ),
    )

    fun buildAliasList(hasPrimary: Boolean = true): List<SysconAlias> = listOf(
      SysconAlias(
        nomisAliasId = randomCId().toLong(),
        titleCode = randomTitleCode().value.name,
        firstName = randomName(),
        middleNames = randomName(),
        lastName = randomName(),
        dateOfBirth = LocalDate.now().minusYears((30..70).random().toLong()),
        sexCode = SexCode.entries.random(),
        isPrimary = hasPrimary,
        identifiers = listOf(
          SysconIdentifier(
            nomisIdentifierId = randomCId().toLong(),
            type = SysconIdentifierType.PNC,
            value = randomName(),
            comment = randomName(),
          ),
        ),
      ),
    )
  }
}
