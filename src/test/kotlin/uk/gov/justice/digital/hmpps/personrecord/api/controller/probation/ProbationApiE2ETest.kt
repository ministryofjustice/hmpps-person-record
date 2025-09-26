package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ContactDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationApiE2ETest : E2ETestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return ok for a create`() {
      val defendantId = randomDefendantId()
      val defendant = createPersonWithNewKey(createRandomCommonPlatformPersonDetails(defendantId))

      val title = TitleCode.MR
      val firstName = randomName()
      val middleName = randomName()
      val lastName = randomName()

      val crn = randomCrn()
      val pnc = randomPnc()
      val cro = randomCro()

      val dateOfBirth = randomDate()

      val aliasFirstName = randomName()
      val aliasMiddleName = randomName()
      val aliasLastName = randomName()
      val aliasDateOfBirth = randomDate()

      val telephone = randomPhoneNumber()
      val mobile = randomPhoneNumber()
      val email = randomEmail()

      val noFixedAbode = false
      val startDate = randomDate()
      val endDate = randomDate()
      val postcode = randomPostcode()
      val fullAddress = randomFullAddress()

      val nationality = randomProbationNationalityCode()
      val ethnicity = randomProbationEthnicity()

      val gender = "M"

      val probationCase = ProbationCase(
        title = Value(value = title.name),
        name = ProbationCaseName(
          firstName = firstName,
          middleNames = middleName,
          lastName = lastName,
        ),
        identifiers = Identifiers(
          crn = crn,
          pnc = pnc,
          cro = cro,
        ),
        dateOfBirth = dateOfBirth,
        aliases = listOf(
          ProbationCaseAlias(
            name = ProbationCaseName(
              firstName = aliasFirstName,
              middleNames = aliasMiddleName,
              lastName = aliasLastName,
            ),
            dateOfBirth = aliasDateOfBirth,
          ),
        ),
        contactDetails = ContactDetails(
          telephone = telephone,
          mobile = mobile,
          email = email,
        ),
        addresses = listOf(
          ProbationAddress(
            noFixedAbode = noFixedAbode,
            startDate = startDate,
            endDate = endDate,
            postcode = postcode,
            fullAddress = fullAddress,
          ),
        ),
        nationality = Value(nationality),
        ethnicity = Value(ethnicity),
        gender = Value(gender),
      )

      webTestClient.put()
        .uri(probationApiUrl(defendantId))
        .authorised(listOf(PROBATION_API_READ_WRITE))
        .bodyValue(probationCase)
        .exchange()
        .expectStatus()
        .isOk

      val offender = awaitNotNullPerson { personRepository.findByCrn(crn) }

      offender.personKey?.assertClusterStatus(ACTIVE)
      offender.personKey?.assertClusterIsOfSize(2)
      offender.assertIncluded(defendant)

      assertThat(offender.getPnc()).isEqualTo(pnc)
      assertThat(offender.crn).isEqualTo(crn)
      assertThat(offender.ethnicityCode?.code).isEqualTo(ethnicity.getProbationEthnicity().code)
      assertThat(offender.ethnicityCode?.description).isEqualTo(ethnicity.getProbationEthnicity().description)
      assertThat(offender.getCro()).isEqualTo(cro)
      assertThat(offender.getAliases().size).isEqualTo(1)
      assertThat(offender.getAliases()[0].firstName).isEqualTo(aliasFirstName)
      assertThat(offender.getAliases()[0].middleNames).isEqualTo(aliasMiddleName)
      assertThat(offender.getAliases()[0].lastName).isEqualTo(aliasLastName)
      assertThat(offender.getAliases()[0].dateOfBirth).isEqualTo(aliasDateOfBirth)
      assertThat(offender.getAliases()[0].nameType).isEqualTo(NameType.ALIAS)
      assertThat(offender.getPrimaryName().firstName).isEqualTo(firstName)
      assertThat(offender.getPrimaryName().middleNames).isEqualTo(middleName)
      assertThat(offender.getPrimaryName().lastName).isEqualTo(lastName)
      assertThat(offender.getPrimaryName().nameType).isEqualTo(NameType.PRIMARY)
      assertThat(offender.getPrimaryName().titleCode?.code).isEqualTo("MR")
      assertThat(offender.getPrimaryName().titleCode?.description).isEqualTo("Mr")
      assertThat(offender.getPrimaryName().dateOfBirth).isEqualTo(dateOfBirth)

      assertThat(offender.addresses.size).isEqualTo(1)
      assertThat(offender.addresses[0].noFixedAbode).isEqualTo(noFixedAbode)
      assertThat(offender.addresses[0].startDate).isEqualTo(startDate)
      assertThat(offender.addresses[0].endDate).isEqualTo(endDate)
      assertThat(offender.addresses[0].postcode).isEqualTo(postcode)
      assertThat(offender.addresses[0].fullAddress).isEqualTo(fullAddress)
      assertThat(offender.addresses[0].type).isEqualTo(null)
      assertThat(offender.contacts.size).isEqualTo(3)
      assertThat(offender.contacts[0].contactType).isEqualTo(ContactType.HOME)
      assertThat(offender.contacts[0].contactValue).isEqualTo(telephone)
      assertThat(offender.contacts[1].contactType).isEqualTo(ContactType.MOBILE)
      assertThat(offender.contacts[1].contactValue).isEqualTo(mobile)
      assertThat(offender.contacts[2].contactType).isEqualTo(ContactType.EMAIL)
      assertThat(offender.contacts[2].contactValue).isEqualTo(email)
      assertThat(offender.matchId).isNotNull()
      assertThat(offender.lastModified).isNotNull()
      assertThat(offender.sexCode).isEqualTo(SexCode.M)
      assertThat(offender.nationalities.size).isEqualTo(1)
      assertThat(offender.nationalities.first().nationalityCode?.code).isEqualTo(nationality.getNationalityCodeEntityFromProbationCode()?.code)
      assertThat(offender.nationalities.first().nationalityCode?.description).isEqualTo(nationality.getNationalityCodeEntityFromProbationCode()?.description)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn),
      )
    }

    @Test
    fun `should set probation and court records that are on different clusters onto same cluster`() {
      val defendantId = randomDefendantId()

      val person = createRandomCommonPlatformPersonDetails(defendantId)
      val defendant = createPersonWithNewKey(person)

      val crn = randomCrn()
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup.from(createRandomProbationPersonDetails(crn)))

      val offender = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(offender.personKey?.personUUID.toString()).isNotEqualTo(defendant.personKey?.personUUID.toString())

      val probationCase = ProbationCase(
        name = ProbationCaseName(firstName = person.firstName, lastName = person.lastName),
        identifiers = Identifiers(crn = crn),
        dateOfBirth = person.dateOfBirth,
      )

      webTestClient.put()
        .uri(probationApiUrl(defendantId))
        .authorised(listOf(PROBATION_API_READ_WRITE))
        .bodyValue(probationCase)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn),
      )

      defendant.personKey?.assertClusterStatus(RECLUSTER_MERGE)
      defendant.personKey?.assertMergedTo(offender.personKey!!)

      offender.personKey?.assertClusterStatus(ACTIVE)
      offender.personKey?.assertClusterIsOfSize(2)
    }

    @Test
    fun `should set include override on existing probation record on the same cluster as the court record`() {
      val basePersonData = createRandomCommonPlatformPersonDetails()

      val defendantId = randomDefendantId()
      val defendant = createPersonWithNewKey(createCommonPlatformPersonFrom(basePersonData, defendantId = defendantId))

      val crn = randomCrn()
      val offenderData = createProbationPersonFrom(basePersonData, crn = crn)
      probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, ApiResponseSetup.from(offenderData))

      val offender = awaitNotNullPerson { personRepository.findByCrn(crn) }

      assertThat(offender.personKey?.personUUID.toString()).isEqualTo(defendant.personKey?.personUUID.toString())

      val probationCase = ProbationCase(
        name = ProbationCaseName(
          firstName = offenderData.firstName,
          middleNames = offenderData.middleNames,
          lastName = offenderData.lastName,
        ),
        identifiers = Identifiers(
          crn = crn,
          pnc = offenderData.getPnc(),
          cro = offenderData.getCro(),
        ),
        dateOfBirth = offenderData.dateOfBirth,
      )

      webTestClient.put()
        .uri(probationApiUrl(defendantId))
        .authorised(listOf(PROBATION_API_READ_WRITE))
        .bodyValue(probationCase)
        .exchange()
        .expectStatus()
        .isOk

      checkTelemetry(
        CPR_RECORD_UPDATED,
        mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to crn),
      )

      offender.personKey?.assertClusterStatus(ACTIVE)
      offender.personKey?.assertClusterIsOfSize(2)

      offender.assertIncluded(defendant)
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return Not Found if defendant does not exist`() {
      val defendantId = randomDefendantId()
      val offender = ProbationCase(
        title = Value(),
        identifiers = Identifiers(crn = randomCrn()),
        name = ProbationCaseName(firstName = randomName()),
        gender = Value(SexCode.M.name),
        ethnicity = Value(randomProbationEthnicity()),
        nationality = Value(randomProbationNationalityCode()),
        contactDetails = ContactDetails(),
      )
      val expectedErrorMessage = "Not found: $defendantId"
      webTestClient.put()
        .uri(probationApiUrl(defendantId))
        .authorised(listOf(PROBATION_API_READ_WRITE))
        .bodyValue(offender)
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val offender = ProbationCase(
        title = Value(),
        identifiers = Identifiers(crn = randomCrn()),
        name = ProbationCaseName(firstName = randomName()),
        gender = Value(SexCode.M.name),
        ethnicity = Value(randomProbationEthnicity()),
        nationality = Value(randomProbationNationalityCode()),
        contactDetails = ContactDetails(),
      )
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.put()
        .uri(probationApiUrl(randomDefendantId()))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .bodyValue(offender)
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.put()
        .uri(probationApiUrl(randomDefendantId()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun probationApiUrl(defendantId: String) = "/person/probation/$defendantId"
}
