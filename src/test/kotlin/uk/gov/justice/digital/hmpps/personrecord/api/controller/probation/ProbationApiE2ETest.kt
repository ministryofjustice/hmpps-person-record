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
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitle
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationApiE2ETest : E2ETestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return ok for a create`() {
      val defendantId = randomDefendantId()

      val defendant = createRandomCommonPlatformPersonDetails(defendantId)
      val probationCase = ProbationCase(
        title = Value(randomTitle()),
        name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
        identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
        dateOfBirth = randomDate(),
        aliases = listOf(ProbationCaseAlias(name = ProbationCaseName(firstName = randomName(), lastName = randomName()))),
        addresses = listOf(ProbationAddress(noFixedAbode = false, startDate = randomDate(), endDate = randomDate(), postcode = randomPostcode(), fullAddress = randomFullAddress())),
        contactDetails = ContactDetails(email = randomEmail(), mobile = randomPhoneNumber(), telephone = randomPhoneNumber()),
        gender = Value(randomProbationSexCode().key),
        nationality = Value(randomProbationNationalityCode()),
      )
      createPersonWithNewKey(defendant)
      webTestClient.put()
        .uri(probationApiUrl(defendantId))
        .authorised(listOf(PROBATION_API_READ_WRITE))
        .bodyValue(probationCase)
        .exchange()
        .expectStatus()
        .isOk

      val offender = awaitNotNullPerson { personRepository.findByCrn(probationCase.identifiers.crn!!) }

      offender.personKey?.assertClusterStatus(ACTIVE)
      offender.personKey?.assertClusterIsOfSize(2)

      assertThat(offender.getPnc()).isEqualTo(probationCase.identifiers.pnc)
      assertThat(offender.ethnicityCode?.code).isEqualTo(probationCase.ethnicity?.value?.getProbationEthnicity()?.code)
      assertThat(offender.ethnicityCode?.code).isEqualTo(probationCase.ethnicity?.value?.getProbationEthnicity()?.description)
      assertThat(offender.getCro()).isEqualTo(probationCase.identifiers.cro)
      assertThat(offender.getAliases().size).isEqualTo(1)
      val firstOffenderAlias = offender.getAliases()[0]
      val firstProbationCaseAlias = probationCase.aliases?.get(0)
      assertThat(firstOffenderAlias.firstName).isEqualTo(firstProbationCaseAlias?.name?.firstName)
      assertThat(firstOffenderAlias.middleNames).isEqualTo(firstProbationCaseAlias?.name?.middleNames)
      assertThat(firstOffenderAlias.lastName).isEqualTo(firstProbationCaseAlias?.name?.lastName)
      assertThat(firstOffenderAlias.dateOfBirth).isEqualTo(firstProbationCaseAlias?.dateOfBirth)
      assertThat(firstOffenderAlias.nameType).isEqualTo(ALIAS)
      assertThat(offender.getPrimaryName().firstName).isEqualTo(probationCase.name.firstName)
      assertThat(offender.getPrimaryName().middleNames).isEqualTo(probationCase.name.middleNames)
      assertThat(offender.getPrimaryName().lastName).isEqualTo(probationCase.name.lastName)
      assertThat(offender.getPrimaryName().nameType).isEqualTo(PRIMARY)
      assertThat(offender.getPrimaryName().titleCode?.code).isEqualTo(probationCase.title?.value?.getTitle()?.code)
      assertThat(offender.getPrimaryName().titleCode?.description).isEqualTo(probationCase.title?.value?.getTitle()?.description)
      assertThat(offender.getPrimaryName().dateOfBirth).isEqualTo(probationCase.dateOfBirth)

      assertThat(offender.addresses.size).isEqualTo(1)
      assertThat(offender.addresses[0].noFixedAbode).isEqualTo(probationCase.addresses[0].noFixedAbode)
      assertThat(offender.addresses[0].startDate).isEqualTo(probationCase.addresses[0].startDate)
      assertThat(offender.addresses[0].endDate).isEqualTo(probationCase.addresses[0].endDate)
      assertThat(offender.addresses[0].postcode).isEqualTo(probationCase.addresses[0].postcode)
      assertThat(offender.addresses[0].fullAddress).isEqualTo(probationCase.addresses[0].fullAddress)
      assertThat(offender.addresses[0].type).isEqualTo(null)
      assertThat(offender.contacts.size).isEqualTo(3)
      assertThat(offender.contacts[0].contactType).isEqualTo(HOME)
      assertThat(offender.contacts[0].contactValue).isEqualTo(probationCase.contactDetails?.telephone)
      assertThat(offender.contacts[1].contactType).isEqualTo(MOBILE)
      assertThat(offender.contacts[1].contactValue).isEqualTo(probationCase.contactDetails?.mobile)
      assertThat(offender.contacts[2].contactType).isEqualTo(EMAIL)
      assertThat(offender.contacts[2].contactValue).isEqualTo(probationCase.contactDetails?.email)
      assertThat(offender.matchId).isNotNull()
      assertThat(offender.lastModified).isNotNull()
      assertThat(offender.sexCode).isEqualTo(SexCode.from(probationCase))
      assertThat(offender.nationalities.size).isEqualTo(1)
      assertThat(offender.nationalities.first().nationalityCode?.code).isEqualTo(probationCase.nationality?.value.getNationalityCodeEntityFromProbationCode()?.code)
      assertThat(offender.nationalities.first().nationalityCode?.description).isEqualTo(probationCase.nationality?.value.getNationalityCodeEntityFromProbationCode()?.description)

      checkTelemetry(
        CPR_RECORD_CREATED,
        mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to probationCase.identifiers.crn),
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
