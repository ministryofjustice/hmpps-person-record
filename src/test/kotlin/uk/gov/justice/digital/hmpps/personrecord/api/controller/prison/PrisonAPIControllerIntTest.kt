package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `cluster size 1 - returns correct response for canonical record`() {
      val prisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
        .copy(
          references = listOf(
            Reference(
              identifierType = PNC,
              identifierValue = randomLongPnc(),
              comment = randomLowerCaseString(),
            ),
          ),
          contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")),
        )
      val cluster = createPersonKey()
        .addPerson(prisonPerson)

      val actualPeronEntity = cluster.personEntities.first()
      val actualResponseBody = sendGetRequestAsserted<CanonicalRecord>(
        url = prisonApiUrl(prisonNumber),
        roles = listOf(API_READ_ONLY),
        expectedStatus = OK,
      ).returnResult().responseBody!!

      assertThat(actualResponseBody).usingRecursiveComparison().isEqualTo(CanonicalRecord.from(actualPeronEntity))
    }

    @Test
    fun `cluster size 2 - returns specified person in canonical record format - along with all known identifiers across cluster`() {
      val prisonPerson1 = createRandomPrisonPersonDetails(randomPrisonNumber())
        .copy(
          crn = randomCrn(),
          defendantId = randomDefendantId(),
          cId = randomCId(),
          references = listOf(
            Reference(identifierType = CRO, identifierValue = randomCro()),
            Reference(identifierType = PNC, identifierValue = randomLongPnc()),
            Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = randomNationalInsuranceNumber()),
            Reference(identifierType = ARREST_SUMMONS_NUMBER, identifierValue = randomArrestSummonNumber()),
            Reference(identifierType = DRIVER_LICENSE_NUMBER, identifierValue = randomDriverLicenseNumber()),
          ),
          contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")),
        )
      val prisonNumber2 = randomPrisonNumber()
      val prisonPerson2 = createRandomPrisonPersonDetails(prisonNumber2)
        .copy(
          crn = randomCrn(),
          defendantId = randomDefendantId(),
          cId = randomCId(),
          references = listOf(
            Reference(identifierType = CRO, identifierValue = randomCro()),
            Reference(identifierType = PNC, identifierValue = randomLongPnc()),
            Reference(identifierType = NATIONAL_INSURANCE_NUMBER, identifierValue = randomNationalInsuranceNumber()),
            Reference(identifierType = ARREST_SUMMONS_NUMBER, identifierValue = randomArrestSummonNumber()),
            Reference(identifierType = DRIVER_LICENSE_NUMBER, identifierValue = randomDriverLicenseNumber()),
          ),
          contacts = listOf(Contact(ContactType.EMAIL, randomEmail())),
        )
      val cluster = createPersonKey()
        .addPerson(prisonPerson1)
        .addPerson(prisonPerson2)

      val actualNewestPeronEntity = cluster.personEntities.first { it.prisonNumber == prisonNumber2 }
      val actualResponseBody = sendGetRequestAsserted<CanonicalRecord>(
        url = prisonApiUrl(prisonNumber2),
        roles = listOf(API_READ_ONLY),
        expectedStatus = OK,
      ).returnResult().responseBody!!

      val expectedCanonicalRecord = CanonicalRecord.from(actualNewestPeronEntity)
      assertThat(actualResponseBody).usingRecursiveComparison().isEqualTo(expectedCanonicalRecord)

      assertThat(actualResponseBody.identifiers.cros).containsExactly(prisonPerson2.getCro())
      assertThat(actualResponseBody.identifiers.pncs).containsExactly(prisonPerson2.getPnc())
      assertThat(actualResponseBody.identifiers.nationalInsuranceNumbers).containsExactly(
        prisonPerson2.references.filter { it.identifierType == NATIONAL_INSURANCE_NUMBER }.map { it.identifierValue }.first(),
      )
      assertThat(actualResponseBody.identifiers.arrestSummonsNumbers).containsExactly(
        prisonPerson2.references.filter { it.identifierType == ARREST_SUMMONS_NUMBER }.map { it.identifierValue }.first(),
      )
      assertThat(actualResponseBody.identifiers.driverLicenseNumbers).containsExactly(
        prisonPerson2.references.filter { it.identifierType == DRIVER_LICENSE_NUMBER }.map { it.identifierValue }.first(),
      )
      assertThat(actualResponseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.crn })
      assertThat(actualResponseBody.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.defendantId })
      assertThat(actualResponseBody.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.prisonNumber })
      assertThat(actualResponseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.cId })
    }

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
