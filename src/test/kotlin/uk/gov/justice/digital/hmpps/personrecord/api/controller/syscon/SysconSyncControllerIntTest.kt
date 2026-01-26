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
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Name
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Sentence
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexCode
import java.time.LocalDate

class SysconSyncControllerIntTest : WebTestBase() {

  @Nested
  inner class Upsert {
    @Test
    fun `person record does not exists - saves record - returns correct response`() {
      stubPersonMatchUpsert()
      stubNoMatchesPersonMatch()

      val prisonNumber = randomPrisonNumber()
      val prisonerRequest = buildRequestBody(prisonNumber)

      webTestClient
        .post()
        .uri("/syscon-sync/$prisonNumber")
        .body(Mono.just(prisonerRequest), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .assertDatabase(prisonNumber, prisonerRequest)
        .expectStatus()
        .isOk
    }

    @Test
    fun `person record does exists - updates record - returns correct response`() {
      stubPersonMatchUpsert()

      val prisonNumber = randomPrisonNumber()
      createPerson(Person.from(buildRequestBody(prisonNumber)))

      val updatedPrisonerRequest = buildRequestBody(prisonNumber)
      webTestClient
        .post()
        .uri("/syscon-sync/$prisonNumber")
        .body(Mono.just(updatedPrisonerRequest), Prisoner::class.java)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .assertDatabase(prisonNumber, updatedPrisonerRequest)
        .expectStatus()
        .isOk
    }
  }

  @Nested
  inner class Auth {
    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val prisonerNumber = randomPrisonNumber()
      val prisonerRequest = buildRequestBody(prisonerNumber)
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri("/syscon-sync/$prisonerNumber")
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
      val prisonerRequest = buildRequestBody(prisonerNumber)
      webTestClient.post()
        .uri("/syscon-sync/$prisonerNumber")
        .body(Mono.just(prisonerRequest), Prisoner::class.java)
        .exchange()
        .assertDatabase(prisonerNumber, prisonerRequest, write = false)
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun WebTestClient.ResponseSpec.assertDatabase(prisonerNumber: String, request: Prisoner, write: Boolean = true): WebTestClient.ResponseSpec {
    if (write) {
      val actualPerson = personRepository.findByPrisonNumber(prisonerNumber)?.let { Person.from(it) } ?: fail { "Prisoner record was expected to be found" }
      val expectedPerson = Person.from(request).copy(personId = actualPerson.personId)
      assertThat { actualPerson == expectedPerson }
    } else {
      assertThat(personRepository.findByPrisonNumber(prisonerNumber)).isNull()
    }
    return this
  }

  private fun buildRequestBody(prisonerNumber: String = randomPrisonNumber()): Prisoner = Prisoner(
    name = Name(
      titleCode = "MR",
      firstName = randomName(),
      middleNames = randomName(),
      lastName = randomName(),
    ),
    demographicAttributes = DemographicAttributes(
      dateOfBirth = randomDate(),
      birthPlace = randomName(),
      birthCountryCode = randomName(),
      ethnicityCode = randomPrisonEthnicity(),
      sexCode = SexCode.M.name,
    ),
    aliases = listOf(
      Alias(
        titleCode = "MISS",
        firstName = randomName(),
        middleNames = randomName(),
        lastName = randomName(),
        dateOfBirth = LocalDate.now().minusYears((30..70).random().toLong()),
        sexCode = randomProbationSexCode().value.name, // NOTE: !!!
      ),
    ),
    addresses = listOf(
      Address(
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
            addressUsageCode = AddressUsageCode.HOME,
            isActive = true,
          ),
        ),
      ),
    ),
    contacts = listOf(
      Contact(
        value = randomPhoneNumber(),
        type = ContactType.entries.random(),
        extension = null,
        isPersonContact = randomBoolean(),
        isAddressContact = randomBoolean(),
      ),
    ),
    identifiers = listOf(
      Identifier(
        type = IdentifierType.PNC,
        value = prisonerNumber,
      ),
    ),
    sentences = listOf(
      Sentence(
        sentenceDate = randomDate(),
      ),
    ),
  )
}
