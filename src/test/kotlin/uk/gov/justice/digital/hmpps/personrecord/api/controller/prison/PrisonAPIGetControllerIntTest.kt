package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonsNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonAPIGetControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `cluster size 1 - returns correct response for canonical record`() {
      val prisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
        .copy(
          contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")),
          nationalities = listOf(randomNationalityCode()),
        )
      val cluster = createPersonKey()
        .addPerson(prisonPerson)

      val person = cluster.personEntities.first()
      val responseBody = sendGetRequestAsserted<CanonicalRecord>(
        url = prisonApiUrl(prisonNumber),
        roles = listOf(API_READ_ONLY),
        expectedStatus = OK,
      ).returnResult().responseBody!!
      val alias = prisonPerson.aliases.first()
      val canonicalAlias = CanonicalAlias(
        firstName = alias.firstName,
        lastName = alias.lastName,
        middleNames = alias.middleNames,
        title = CanonicalTitle.from(alias.titleCode),
        sex = CanonicalSex.from(alias.sexCode),
      )
      val nationality = prisonPerson.nationalities.first()
      val canonicalNationality = listOf(CanonicalNationality(nationality.name, nationality.description))
      val address = prisonPerson.addresses.first()
      val canonicalAddress = CanonicalAddress(
        noFixedAbode = address.noFixedAbode,
        startDate = address.startDate.toString(),
        endDate = address.endDate?.toString(),
        postcode = address.postcode,
        buildingName = address.buildingName,
        buildingNumber = address.buildingNumber,
        thoroughfareName = address.thoroughfareName,
        dependentLocality = address.dependentLocality,
        postTown = address.postTown,
      )
      val address2 = prisonPerson.addresses.get(1)
      val canonicalAddress2 = CanonicalAddress(
        noFixedAbode = address2.noFixedAbode,
        startDate = address2.startDate.toString(),
        endDate = address2.endDate?.toString(),
        postcode = address2.postcode,
        buildingName = address2.buildingName,
        buildingNumber = address2.buildingNumber,
        thoroughfareName = address2.thoroughfareName,
        dependentLocality = address2.dependentLocality,
        postTown = address2.postTown,
      )

      val canonicalReligion = CanonicalReligion(code = prisonPerson.religion, description = prisonPerson.religion)
      val canonicalEthnicity = CanonicalEthnicity.from(prisonPerson.ethnicityCode)
      assertThat(responseBody.cprUUID).isNull()
      assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      assertThat(responseBody.disability).isEqualTo(person.disability)
      assertThat(responseBody.interestToImmigration).isEqualTo(person.immigrationStatus)
      assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.name)
      assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.name)
      assertThat(responseBody.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
      assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
      assertThat(responseBody.aliases.first().sex.code).isEqualTo(canonicalAlias.sex.code)
      assertThat(responseBody.aliases.first().sex.description).isEqualTo(canonicalAlias.sex.description)

      assertThat(responseBody.sexualOrientation.code).isEqualTo(prisonPerson.sexualOrientation?.name)
      assertThat(responseBody.sexualOrientation.description).isEqualTo(prisonPerson.sexualOrientation?.description)
      assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
      assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
      assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
      assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
      assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
      assertThat(responseBody.identifiers.cros).isEqualTo(listOf(prisonPerson.getCro()))
      assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(prisonPerson.getPnc()))
      assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
      assertThat(responseBody.addresses).usingRecursiveComparison().isEqualTo(listOf(canonicalAddress, canonicalAddress2))
    }

    @Test
    fun `should add list of additional identifiers to the prison record`() {
      val personOneCro = randomCro()
      val personOnePnc = randomLongPnc()
      val personOneNationalInsuranceNumber = randomNationalInsuranceNumber()
      val personOneArrestSummonNumber = randomArrestSummonsNumber()
      val personOneDriversLicenseNumber = randomDriverLicenseNumber()

      val personOnePrison = createPerson(
        createRandomPrisonPersonDetails().copy(
          references = listOf(
            Reference(identifierType = CRO, identifierValue = personOneCro),
            Reference(identifierType = PNC, identifierValue = personOnePnc),
            Reference(
              identifierType = NATIONAL_INSURANCE_NUMBER,
              identifierValue = personOneNationalInsuranceNumber,
            ),
            Reference(
              identifierType = ARREST_SUMMONS_NUMBER,
              identifierValue = personOneArrestSummonNumber,
            ),
            Reference(
              identifierType = DRIVER_LICENSE_NUMBER,
              identifierValue = personOneDriversLicenseNumber,
            ),
          ),
        ),
      )

      val personTwoProbation = createPerson(
        createRandomProbationPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )

      val personThreeCommonPlatform = createPerson(
        createRandomCommonPlatformPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )
      val personFourLibra = createPerson(
        createRandomLibraPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )
      val personFivePrison = createPerson(
        createRandomPrisonPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )

      val personSixProbation = createPerson(
        createRandomProbationPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )

      val personSevenCommonPlatform = createPerson(
        createRandomCommonPlatformPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )
      val personEightLibra = createPerson(
        createRandomLibraPersonDetails().copy(
          references = randomFullSetOfReferences(),
        ),
      )

      createPersonKey()
        .addPerson(personOnePrison)
        .addPerson(personTwoProbation)
        .addPerson(personThreeCommonPlatform)
        .addPerson(personFourLibra)
        .addPerson(personFivePrison)
        .addPerson(personSixProbation)
        .addPerson(personSevenCommonPlatform)
        .addPerson(personEightLibra)

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(personOnePrison.prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.identifiers.cros).containsExactly(personOneCro)
      assertThat(responseBody.identifiers.pncs).containsExactly(personOnePnc)
      assertThat(responseBody.identifiers.nationalInsuranceNumbers).containsExactly(personOneNationalInsuranceNumber)
      assertThat(responseBody.identifiers.arrestSummonsNumbers).containsExactly(personOneArrestSummonNumber)
      assertThat(responseBody.identifiers.driverLicenseNumbers).containsExactly(personOneDriversLicenseNumber)
      assertThat(responseBody.identifiers.crns).containsExactly(personTwoProbation.crn, personSixProbation.crn)
      assertThat(responseBody.identifiers.defendantIds).containsExactly(personThreeCommonPlatform.defendantId, personSevenCommonPlatform.defendantId)
      assertThat(responseBody.identifiers.prisonNumbers).containsExactly(personOnePrison.prisonNumber, personFivePrison.prisonNumber)
      assertThat(responseBody.identifiers.cids).containsExactly(personFourLibra.cId, personEightLibra.cId)
    }

    private fun randomFullSetOfReferences(): List<Reference> = listOf(
      Reference(identifierType = CRO, identifierValue = randomCro()),
      Reference(identifierType = PNC, identifierValue = randomLongPnc()),
      Reference(
        identifierType = NATIONAL_INSURANCE_NUMBER,
        identifierValue = randomNationalInsuranceNumber(),
      ),
      Reference(
        identifierType = ARREST_SUMMONS_NUMBER,
        identifierValue = randomArrestSummonsNumber(),
      ),
      Reference(
        identifierType = DRIVER_LICENSE_NUMBER,
        identifierValue = randomDriverLicenseNumber(),
      ),
    )

    @Test
    fun `should return redirect when the requested prison record has been merged`() {
      val sourcePrisonNumber = randomPrisonNumber()
      val targetPrisonNumber = randomPrisonNumber()

      val sourcePersonEntity = createPerson(createRandomPrisonPersonDetails(sourcePrisonNumber))
      val targetPersonEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(targetPrisonNumber))

      mergeRecord(sourcePersonEntity, targetPersonEntity)

      webTestClient.get()
        .uri(prisonApiUrl(sourcePrisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .is3xxRedirection
        .expectHeader()
        .valueEquals("Location", "/person/prison/$targetPrisonNumber")
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return not found 404 with userMessage to show that the prisonNumber is not found`() {
      val prisonNumber = randomPrisonNumber()
      val expectedErrorMessage = "Not found: $prisonNumber"
      webTestClient.get()
        .uri(prisonApiUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(prisonApiUrl("accessdenied"))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.get()
        .uri(prisonApiUrl("unauthorised"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun prisonApiUrl(prisonNumber: String?) = "/person/prison/$prisonNumber"
}
