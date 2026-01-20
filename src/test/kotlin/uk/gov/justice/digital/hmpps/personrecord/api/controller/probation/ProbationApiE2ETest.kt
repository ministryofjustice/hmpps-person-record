package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ContactDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getEmail
import uk.gov.justice.digital.hmpps.personrecord.extensions.getHome
import uk.gov.justice.digital.hmpps.personrecord.extensions.getMobile
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_PERSONAL_DETAILS_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ProbationApiE2ETest : E2ETestBase() {

  @Nested
  inner class GetRecord {

    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should return ok for get`() {
        val firstName = randomName()
        val secondAliasFirstName = randomName()
        val lastName = randomName()
        val secondAliasLastName = randomName()
        val middleNames = randomName()
        val secondAliasMiddleName = randomName()
        val title = randomTitleCode()
        val pnc = randomLongPnc()
        val noFixedAbode = true
        val startDate = randomDate()
        val endDate = randomDate()
        val postcode = randomPostcode()
        val nationality = randomNationalityCode()
        val religion = randomReligion()
        val ethnicity = randomCommonPlatformEthnicity()
        val primarySex = randomProbationSexCode()
        val aliasSex1 = randomProbationSexCode()
        val aliasSex2 = randomProbationSexCode()
        val sexualOrientation = randomPrisonSexualOrientation().value

        val buildingName = randomName()
        val buildingNumber = randomBuildingNumber()
        val thoroughfareName = randomName()
        val dependentLocality = randomName()
        val postTown = randomName()

        val cro = randomCro()
        val crn = randomCrn()
        val prisonNumber = randomPrisonNumber()

        val person = createPersonWithNewKey(
          Person(
            firstName = randomName(),
            lastName = randomName(),
            middleNames = randomName(),
            dateOfBirth = randomDate(),
            disability = randomBoolean(),
            immigrationStatus = randomBoolean(),
            sourceSystem = NOMIS,
            titleCode = title.value,
            crn = crn,
            sexCode = primarySex.value,
            sexualOrientation = sexualOrientation,
            prisonNumber = prisonNumber,
            nationalities = listOf(nationality),
            religion = religion,
            ethnicityCode = EthnicityCode.fromCommonPlatform(ethnicity),
            aliases = listOf(
              Alias(
                firstName = firstName,
                middleNames = middleNames,
                lastName = lastName,
                dateOfBirth = randomDate(),
                titleCode = title.value,
                sexCode = aliasSex1.value,
              ),
              Alias(
                firstName = secondAliasFirstName,
                middleNames = secondAliasMiddleName,
                lastName = secondAliasLastName,
                dateOfBirth = randomDate(),
                titleCode = title.value,
                sexCode = aliasSex2.value,
              ),
            ),
            addresses = listOf(
              Address(
                noFixedAbode = noFixedAbode,
                startDate = startDate,
                endDate = endDate,
                postcode = postcode,
                buildingName = buildingName,
                buildingNumber = buildingNumber,
                thoroughfareName = thoroughfareName,
                dependentLocality = dependentLocality,
                postTown = postTown,
              ),
            ),
            references = listOf(
              Reference(identifierType = IdentifierType.PNC, identifierValue = pnc),
              Reference(identifierType = IdentifierType.CRO, identifierValue = cro),
            ),
          ),
        )

        val responseBody = webTestClient.get()
          .uri(probationApiUrl(crn))
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody(CanonicalRecord::class.java)
          .returnResult()
          .responseBody!!

        val canonicalAliases = listOf(
          CanonicalAlias(
            firstName = firstName,
            lastName = lastName,
            middleNames = middleNames,
            title = CanonicalTitle.from(title.value),
            sex = CanonicalSex.from(aliasSex1.value),
          ),
          CanonicalAlias(
            firstName = secondAliasFirstName,
            lastName = secondAliasLastName,
            middleNames = secondAliasMiddleName,
            title = CanonicalTitle.from(title.value),
            sex = CanonicalSex.from(aliasSex2.value),
          ),
        )
        val canonicalNationality = listOf(CanonicalNationality(nationality.name, nationality.description))
        val canonicalAddress = CanonicalAddress(
          noFixedAbode = noFixedAbode,
          startDate = startDate.toString(),
          endDate = endDate.toString(),
          postcode = postcode,
          buildingName = buildingName,
          buildingNumber = buildingNumber,
          thoroughfareName = thoroughfareName,
          dependentLocality = dependentLocality,
          postTown = postTown,
        )
        val canonicalReligion = CanonicalReligion(code = religion, description = religion)
        val canonicalEthnicity = CanonicalEthnicity.from(EthnicityCode.fromProbation(ethnicity))
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
        assertThat(responseBody.aliases.first().sex.code).isEqualTo(aliasSex1.value.name)
        assertThat(responseBody.aliases.first().sex.description).isEqualTo(aliasSex1.value.description)
        assertThat(responseBody.aliases.last().sex.code).isEqualTo(aliasSex2.value.name)
        assertThat(responseBody.aliases.last().sex.description).isEqualTo(aliasSex2.value.description)
        assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
        assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
        assertThat(responseBody.sex.code).isEqualTo(primarySex.value.name)
        assertThat(responseBody.sex.description).isEqualTo(primarySex.value.description)
        assertThat(responseBody.sexualOrientation.code).isEqualTo(sexualOrientation.name)
        assertThat(responseBody.sexualOrientation.description).isEqualTo(sexualOrientation.description)
        assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
        assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
        assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
        assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
        assertThat(responseBody.aliases).isEqualTo(canonicalAliases)
        assertThat(responseBody.identifiers.cros).isEqualTo(listOf(cro))
        assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(pnc))
        assertThat(responseBody.identifiers.crns).isEqualTo(listOf(crn))
        assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
        assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
      }

      @Test
      fun `should add list of additional identifiers to the canonical record`() {
        val personOneCro = randomCro()
        val personTwoCro = randomCro()

        val personOneCrn = randomCrn()
        val personTwoCrn = randomCrn()

        val personOnePnc = randomLongPnc()
        val personTwoPnc = randomLongPnc()

        val personOneNationalInsuranceNumber = randomNationalInsuranceNumber()
        val personTwoNationalInsuranceNumber = randomNationalInsuranceNumber()

        val personOneArrestSummonNumber = randomArrestSummonNumber()
        val personTwoArrestSummonNumber = randomArrestSummonNumber()

        val personOneDriversLicenseNumber = randomDriverLicenseNumber()
        val personTwoDriversLicenseNumber = randomDriverLicenseNumber()

        val personOneDefendantId = randomDefendantId()
        val personTwoDefendantId = randomDefendantId()

        val personOne = createPerson(
          Person(
            firstName = randomName(),
            lastName = randomName(),
            middleNames = randomName(),
            dateOfBirth = randomDate(),
            sourceSystem = NOMIS,
            crn = personOneCrn,
            prisonNumber = randomPrisonNumber(),
            nationalities = listOf(randomNationalityCode()),
            religion = randomReligion(),
            cId = randomCId(),
            defendantId = personOneDefendantId,
            masterDefendantId = personOneDefendantId,
            references = listOf(
              Reference(identifierType = IdentifierType.CRO, identifierValue = personOneCro),
              Reference(identifierType = IdentifierType.PNC, identifierValue = personOnePnc),
              Reference(
                identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
                identifierValue = personOneNationalInsuranceNumber,
              ),
              Reference(
                identifierType = IdentifierType.ARREST_SUMMONS_NUMBER,
                identifierValue = personOneArrestSummonNumber,
              ),
              Reference(
                identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
                identifierValue = personOneDriversLicenseNumber,
              ),
            ),
          ),
        )

        val personTwo = createPerson(
          Person(
            firstName = randomName(),
            lastName = randomName(),
            middleNames = randomName(),
            dateOfBirth = randomDate(),
            sourceSystem = NOMIS,
            crn = personTwoCrn,
            prisonNumber = randomPrisonNumber(),
            nationalities = listOf(randomNationalityCode()),
            religion = randomReligion(),
            cId = randomCId(),
            defendantId = personTwoDefendantId,
            masterDefendantId = personTwoDefendantId,
            references = listOf(
              Reference(identifierType = IdentifierType.CRO, identifierValue = personTwoCro),
              Reference(identifierType = IdentifierType.PNC, identifierValue = personTwoPnc),
              Reference(
                identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
                identifierValue = personTwoNationalInsuranceNumber,
              ),
              Reference(
                identifierType = IdentifierType.ARREST_SUMMONS_NUMBER,
                identifierValue = personTwoArrestSummonNumber,
              ),
              Reference(
                identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
                identifierValue = personTwoDriversLicenseNumber,
              ),
            ),
          ),
        )
        createPersonKey().addPerson(personOne).addPerson(personTwo)
        val responseBody = webTestClient.get()
          .uri(probationApiUrl(personOneCrn))
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
        assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(
          listOf(
            personOne.crn,
            personTwo.crn,
          ),
        )
        assertThat(responseBody.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(
          listOf(
            personOne.defendantId,
            personTwo.defendantId,
          ),
        )
        assertThat(responseBody.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(
          listOf(
            personOne.prisonNumber,
            personTwo.prisonNumber,
          ),
        )
        assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(
          listOf(
            personOne.cId,
            personTwo.cId,
          ),
        )
      }
    }

    @Nested
    inner class ErrorScenarios {

      @Test
      fun `should return not found 404 with userMessage to show that the prisonNumber is not found`() {
        val crn = randomCrn()
        val expectedErrorMessage = "Not found: $crn"
        webTestClient.get()
          .uri(probationApiUrl(crn))
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
          .uri(probationApiUrl("accessdenied"))
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
          .uri(probationApiUrl("unauthorised"))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class PutRecord {
    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should return ok for a create`() {
        val defendantId = randomDefendantId()

        val defendant = createRandomCommonPlatformPersonDetails(defendantId)
        val title = randomTitleCode()
        val probationCase = ProbationCase(
          title = Value(title.key),
          name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
          identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
          dateOfBirth = randomDate(),
          aliases = listOf(
            ProbationCaseAlias(
              name = ProbationCaseName(
                firstName = randomName(),
                lastName = randomName(),
              ),
            ),
          ),
          addresses = listOf(
            ProbationAddress(
              noFixedAbode = false,
              startDate = randomDate(),
              endDate = randomDate(),
              postcode = randomPostcode(),
              fullAddress = randomFullAddress(),
            ),
          ),
          ethnicity = Value(randomProbationEthnicity()),
          contactDetails = ContactDetails(
            email = randomEmail(),
            mobile = randomPhoneNumber(),
            telephone = randomPhoneNumber(),
          ),
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

        val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }

        offender.personKey?.assertClusterStatus(ACTIVE)
        offender.personKey?.assertClusterIsOfSize(2)

        assertThat(offender.getPnc()).isEqualTo(probationCase.identifiers.pnc)
        assertThat(offender.ethnicityCode).isEqualTo(EthnicityCode.fromProbation(probationCase.ethnicity?.value))
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
        assertThat(offender.getPrimaryName().titleCode).isEqualTo(title.value)
        assertThat(offender.getPrimaryName().dateOfBirth).isEqualTo(probationCase.dateOfBirth)
        assertThat(offender.getPrimaryName().sexCode).isEqualTo(SexCode.from(probationCase))

        assertThat(offender.addresses.size).isEqualTo(1)
        assertThat(offender.addresses[0].noFixedAbode).isEqualTo(probationCase.addresses[0].noFixedAbode)
        assertThat(offender.addresses[0].startDate).isEqualTo(probationCase.addresses[0].startDate)
        assertThat(offender.addresses[0].endDate).isEqualTo(probationCase.addresses[0].endDate)
        assertThat(offender.addresses[0].postcode).isEqualTo(probationCase.addresses[0].postcode)
        assertThat(offender.addresses[0].fullAddress).isEqualTo(probationCase.addresses[0].fullAddress)
        assertThat(offender.addresses[0].type).isEqualTo(null)
        assertThat(offender.contacts.size).isEqualTo(3)
        assertThat(offender.contacts.getHome()?.contactValue).isEqualTo(probationCase.contactDetails?.telephone)
        assertThat(offender.contacts.getMobile()?.contactValue).isEqualTo(probationCase.contactDetails?.mobile)
        assertThat(offender.contacts.getEmail()?.contactValue).isEqualTo(probationCase.contactDetails?.email)
        assertThat(offender.matchId).isNotNull()
        assertThat(offender.lastModified).isNotNull()
        assertThat(offender.nationalities.size).isEqualTo(1)
        assertThat(offender.nationalities.first().nationalityCode?.name).isEqualTo(NationalityCode.fromProbationMapping(probationCase.nationality?.value)?.name)
        assertThat(offender.nationalities.first().nationalityCode?.description).isEqualTo(NationalityCode.fromProbationMapping(probationCase.nationality?.value)?.description)

        checkTelemetry(
          CPR_RECORD_CREATED,
          mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to probationCase.identifiers.crn),
        )
      }

      @Test
      fun `should retain master defendant id on update on probation record`() {
        val defendantId = randomDefendantId()

        val defendant = createRandomCommonPlatformPersonDetails(defendantId)
        val probationCase = ProbationCase(
          name = ProbationCaseName(firstName = defendant.firstName, lastName = defendant.lastName),
          identifiers = Identifiers(crn = randomCrn(), cro = defendant.getCro(), pnc = defendant.getPnc()),
          dateOfBirth = defendant.dateOfBirth,
        )
        createPersonWithNewKey(defendant)
        webTestClient.put()
          .uri(probationApiUrl(defendantId))
          .authorised(listOf(PROBATION_API_READ_WRITE))
          .bodyValue(probationCase)
          .exchange()
          .expectStatus()
          .isOk

        val offender = awaitNotNull { personRepository.findByCrn(probationCase.identifiers.crn!!) }

        offender.personKey?.assertClusterStatus(ACTIVE)
        offender.personKey?.assertClusterIsOfSize(2)

        probationDomainEventAndResponseSetup(
          OFFENDER_PERSONAL_DETAILS_UPDATED,
          ApiResponseSetup.from(probationCase.aboveFracture()),
        )

        checkTelemetry(
          CPR_RECORD_UPDATED,
          mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to probationCase.identifiers.crn),
        )
        offender.personKey?.assertClusterStatus(ACTIVE)
        offender.personKey?.assertClusterIsOfSize(2)
        assertThat(offender.masterDefendantId).isEqualTo(defendant.masterDefendantId)
      }

      @Test
      fun `should set probation and court records that are on different clusters onto same cluster`() {
        val defendantId = randomDefendantId()

        val person = createRandomCommonPlatformPersonDetails(defendantId)
        val defendant = createPersonWithNewKey(person)

        val crn = randomCrn()
        probationDomainEventAndResponseSetup(
          NEW_OFFENDER_CREATED,
          ApiResponseSetup.from(createRandomProbationCase(crn)),
        )

        val offender = awaitNotNull { personRepository.findByCrn(crn) }

        assertThat(offender.personKey?.personUUID.toString()).isNotEqualTo(defendant.personKey?.personUUID.toString())

        val probationCase = ProbationCase(
          name = ProbationCaseName(firstName = person.firstName, lastName = person.lastName),
          identifiers = Identifiers(crn = crn, pnc = person.getPnc(), cro = person.getCro()),
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
          identifiers = Identifiers(crn = randomCrn()),
          name = ProbationCaseName(firstName = randomName()),
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
          identifiers = Identifiers(crn = randomCrn()),
          name = ProbationCaseName(firstName = randomName()),
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
  }

  private fun probationApiUrl(defendantId: String) = "/person/probation/$defendantId"
}
