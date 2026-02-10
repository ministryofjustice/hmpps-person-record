package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Address
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Alias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Contact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.DemographicAttributes
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Identifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Sentence
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
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
import java.time.LocalDate

class SysconSyncControllerIntTest : WebTestBase() {

  @Nested
  inner class Update {
    @Test
    fun `person record does exists - updates record - returns correct response`() {
      stubPersonMatchUpsert()

      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val updatedPrisonerRequest = buildRequestBody()
      webTestClient
        .put()
        .uri("/syscon-sync/person/$prisonNumber")
        .body(Mono.just(updatedPrisonerRequest), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .assertDatabase(prisonNumber, updatedPrisonerRequest)
        .expectStatus()
        .isOk
    }
  }

  @Nested
  inner class BadRequest {
    @Test
    fun `person record does not exists - does not insert - returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      val prisonerRequest = buildRequestBody()

      webTestClient
        .put()
        .uri("/syscon-sync/person/$prisonNumber")
        .body(Mono.just(prisonerRequest), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .assertDatabase(prisonNumber, prisonerRequest, write = false)
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `no primary alias is sent - does not update - returns correct response`() {
      val prisonNumber = randomPrisonNumber()
      val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = buildRequestBody().copy(aliases = buildAliasList(false))
      webTestClient
        .put()
        .uri("/syscon-sync/person/$prisonNumber")
        .body(Mono.just(requestBody), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Bad request: No primary alias was found for update on prisoner $prisonNumber")

      val actualPerson = personRepository.findByPrisonNumber(prisonNumber)?.let { Person.from(it) } ?: fail { "Person not found for update on prisoner $prisonNumber" }
      val expectedPerson = Person.from(originalPerson)
      assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
    }
  }

  @Nested
  inner class Auth {
    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val prisonerNumber = randomPrisonNumber()
      val prisonerRequest = buildRequestBody()
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.put()
        .uri("/syscon-sync/person/$prisonerNumber")
        .body(Mono.just(prisonerRequest), Prisoner::class.java)
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .assertDatabase(prisonerNumber, prisonerRequest, write = false)
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val prisonerNumber = randomPrisonNumber()
      val prisonerRequest = buildRequestBody()
      webTestClient.put()
        .uri("/syscon-sync/person/$prisonerNumber")
        .body(Mono.just(prisonerRequest), Prisoner::class.java)
        .exchange()
        .assertDatabase(prisonerNumber, prisonerRequest, write = false)
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun WebTestClient.ResponseSpec.assertDatabase(prisonerNumber: String, request: Prisoner, write: Boolean = true): WebTestClient.ResponseSpec {
    if (write) {
      val actualPersonEntity = personRepository.findByPrisonNumber(prisonerNumber) ?: fail { "Prisoner record was expected to be found" }
      val actualPerson = Person.from(actualPersonEntity)
      val expectedPerson = Person.from(request, prisonerNumber).copy(personId = actualPerson.personId)
      assertThat(actualPerson).usingRecursiveComparison().isEqualTo(expectedPerson)
    } else {
      assertThat(personRepository.findByPrisonNumber(prisonerNumber)).isNull()
    }
    return this
  }

  private fun buildRequestBody(): Prisoner = Prisoner(
    demographicAttributes = DemographicAttributes(
      birthPlace = randomName(),
      birthCountryCode = randomName(),
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
      Address(
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
          AddressUsage(
            nomisAddressUsageId = randomCId().toLong(),
            addressUsageCode = AddressUsageCode.HOME,
            isActive = true,
          ),
        ),
        contacts = listOf(
          Contact(
            nomisContactId = randomCId().toLong(),
            value = randomPhoneNumber(),
            type = ContactType.entries.random(),
            extension = null,
          ),
        ),
      ),
    ),
    personContacts = listOf(
      Contact(
        nomisContactId = randomCId().toLong(),
        value = randomPhoneNumber(),
        type = ContactType.entries.random(),
        extension = null,
      ),
    ),
    sentences = listOf(
      Sentence(
        sentenceDate = randomDate(),
      ),
    ),
  )

  private fun buildAliasList(hasPrimary: Boolean = true): List<Alias> = listOf(
    Alias(
      nomisAliasId = randomCId().toLong(),
      titleCode = randomTitleCode().value.name,
      firstName = randomName(),
      middleNames = randomName(),
      lastName = randomName(),
      dateOfBirth = LocalDate.now().minusYears((30..70).random().toLong()),
      sexCode = SexCode.entries.random(),
      isPrimary = hasPrimary,
      identifiers = listOf(
        Identifier(
          nomisIdentifierId = randomCId().toLong(),
          type = IdentifierType.PNC,
          value = randomName(),
          comment = randomName(),
        ),
      ),
    ),
  )
}
