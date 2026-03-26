package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.SentenceInfoRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicityCode
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
      val expectedResponseBody = SysconUpdatePersonResponse(
        prisonerId = prisonNumber,
        addressMappings = listOf(
          AddressMapping(
            nomisAddressId = updatePrisonerRequestBody.addresses.first().nomisAddressId.toString(),
            cprAddressId = personEntity.addresses.first().updateId.toString(),
            addressUsageMappings = listOf(
              AddressUsageMapping(
                nomisAddressUsageId = updatePrisonerRequestBody.addresses.first().addressUsage.first().nomisAddressUsageId.toString(),
                cprAddressUsageid = personEntity.addresses.first().usages.first().updateId.toString(),
              ),
            ),
            addressContactMappings = listOf(
              AddressContactMapping(
                nomisContactId = updatePrisonerRequestBody.addresses.first().contacts.first().nomisContactId.toString(),
                cprContactId = personEntity.addresses.first().contacts.first().updateId.toString(),
              ),
            ),
          ),
        ),
        personContactMappings = listOf(
          PersonContactMapping(
            nomisContactId = updatePrisonerRequestBody.personContacts.first().nomisContactId.toString(),
            cprContactId = personEntity.contacts.first().updateId.toString(),
          ),
        ),
        pseudonymMappings = listOf(
          AliasMapping(
            nomisPseudonymId = updatePrisonerRequestBody.pseudonyms.first().nomisAliasId.toString(),
            cprPseudonymId = personEntity.pseudonyms.first().updateId.toString(),
            identifierMappings = listOf(
              IdentifierMapping(
                nomisIdentifierId = updatePrisonerRequestBody.pseudonyms.first().identifiers.first().nomisIdentifierId.toString(),
                cprIdentifierId = personEntity.references.first().updateId.toString(),
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
      val aliasList = request.pseudonyms + SysconAlias(
        nomisAliasId = randomCId().toLong(),
        titleCode = randomTitleCode().value,
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
      val updatePrisonerRequestBody = request.copy(pseudonyms = aliasList)

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

    @Test
    fun `no primary alias is sent - does not update - returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val updatedPrisonerRequest = buildRequestBody().copy(pseudonyms = buildAliasList(false))
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

    @Test
    fun `invalid enum code is sent - does not update - returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val json = ObjectMapper().valueToTree<ObjectNode>(buildRequestBody())
      (json.get("demographicAttributes") as ObjectNode).put("ethnicityCode", "TEST")

      webTestClient
        .put()
        .uri("/syscon-sync/person/$prisonNumber")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(json)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("userMessage").value<String> { msg ->
          assertThat(msg)
            .contains("Bad request: JSON parse error")
            .contains("\"TEST\": not one of the values accepted for Enum class")
        }

      val actualPerson = personRepository.findByPrisonNumber(prisonNumber)?.let { Person.from(it) } ?: fail { "Person not found for update on prisoner $prisonNumber" }
      val expectedPerson = Person.from(originalPerson)
      assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
    }
  }

  @Nested
  inner class RubbishData {
    @Test
    fun `empty value for person contact is sent - does not save contact - saves everything else`() {
      val prisonNumber = randomPrisonNumber()
      createRandomPrisonPerson(prisonNumber)

      val updatePrisonerRequestBody = buildRequestBody().copy(
        personContacts = listOf(
          SysconContact(
            nomisContactId = randomCId().toLong(),
            value = " ", // <--- blank contact value
            type = ContactType.entries.random(),
            extension = null,
          ),
        ),
      )
      val actualResponseBody = sendPutRequestAsserted<SysconUpdatePersonResponse>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatePrisonerRequestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      val actualPersonEntity = assertDatabase(prisonNumber, updatePrisonerRequestBody.copy(personContacts = emptyList()))!!
      assertThat(actualPersonEntity.contacts.size).isEqualTo(0)
      assertThat(actualResponseBody.personContactMappings.size).isEqualTo(0)
    }

    @Test
    fun `empty value for address contact is sent - does not save contact - saves everything else`() {
    }

    // TODO: update after 1064
//    @Test
//    fun `no identifiable names sent for alias - does not save pseudonym - saves everything else`() {
//      val prisonNumber = randomPrisonNumber()
//      createRandomPrisonPerson(prisonNumber)
//
//      val onlyExpectedAlias = buildAliasList(hasPrimary = false).first()
//      val onlyExpectedReferences = onlyExpectedAlias.identifiers
//      val updatePrisonerRequestBody = buildRequestBody().copy(
//        aliases = listOf(
//          buildAliasList(hasPrimary = true).first(),
//          buildAliasList().first().copy(
//            isPrimary = false,
//            firstName = " ",
//            middleNames = "",
//            lastName = " ",
//          ),
//          onlyExpectedAlias,
//        )
//      )
//      val actualResponseBody = sendPutRequestAsserted<SysconUpdatePersonResponse>(
//        url = "/syscon-sync/person/$prisonNumber",
//        body = updatePrisonerRequestBody,
//        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
//        expectedStatus = HttpStatus.OK,
//      ).returnResult().responseBody!!
//
//      val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail { "Prisoner record was expected to be found" }
//      val actualPerson = Person.from(actualPersonEntity)
//      val expectedPerson = Person.from(updatePrisonerRequestBody, prisonNumber).copy(
//        personId = actualPerson.personId,
//        aliases = listOf(Alias.from(onlyExpectedAlias)),
//        references = onlyExpectedReferences.map { Reference.from(it) }
//      )
//      assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
//      assertThat(actualPersonEntity.pseudonyms.size).isEqualTo(0)
//      assertThat(actualResponseBody.pseudonymMappings.size).isEqualTo(0)
//    }

    @Test
    fun `no sentence date sent for sentence info - does not save sentence info - saves evertyhing else`() {
      val prisonNumber = randomPrisonNumber()
      createRandomPrisonPerson(prisonNumber)

      val updatePrisonerRequestBody = buildRequestBody().copy(
        sentences = listOf(
          SysconSentence(
            sentenceDate = null,
          ),
        ),
      )
      sendPutRequestAsserted<SysconUpdatePersonResponse>(
        url = "/syscon-sync/person/$prisonNumber",
        body = updatePrisonerRequestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      val actualPersonEntity = assertDatabase(prisonNumber, updatePrisonerRequestBody.copy(sentences = emptyList()))!!
      assertThat(actualPersonEntity.sentenceInfo.size).isEqualTo(0)
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

  private fun assertDatabase(prisonNumber: String, updatedPrisonerRequest: Prisoner, isWriteExpected: Boolean = true): PersonEntity? = if (isWriteExpected) {
    val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail { "Prisoner record was expected to be found" }

    val actualPerson = Person.from(actualPersonEntity)
    val expectedPerson = Person.from(updatedPrisonerRequest, prisonNumber).copy(
      personId = actualPerson.personId,
      aliases = updatedPrisonerRequest.pseudonyms.filter { it.isPrimary == false }.map { Alias.from(it) },
    )
    assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
    actualPersonEntity
  } else {
    val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber)
    assertThat(actualPersonEntity).isNull()
    null
  }

  companion object {
    fun buildRequestBody(): Prisoner = Prisoner(
      demographicAttributes = SysconDemographicAttributes(
        birthPlace = randomName(),
        birthCountryCode = randomCountryCode(),
        ethnicityCode = randomPrisonEthnicityCode(),
        sexCode = randomPrisonSexCode().value,
        sexualOrientation = randomPrisonSexualOrientation().value,
        disability = randomBoolean(),
        interestToImmigration = randomBoolean(),
        religionCode = randomReligionCode(),
        nationalityCode = randomNationalityCode(),
        nationalityNote = randomName(),
      ),
      pseudonyms = buildAliasList(),
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
          countryCode = randomCountryCode(),
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
        titleCode = randomTitleCode().value,
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
