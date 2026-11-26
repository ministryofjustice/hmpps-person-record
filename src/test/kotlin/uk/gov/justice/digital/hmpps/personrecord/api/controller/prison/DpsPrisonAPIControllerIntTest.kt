package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContactType
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.DpsPrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.DpsPrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.HUM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonsNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DpsPrisonAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Autowired
  private lateinit var pseudonymRepository: PseudonymRepository

  @Autowired
  private lateinit var prisonPseudonymRepository: PrisonPseudonymRepository

  @Autowired
  private lateinit var referenceRepository: ReferenceRepository

  @Nested
  @DisplayName("GET /person/prison/dps/{prisonNumber}")
  inner class GetByPrisonNumber {
    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `cluster size 1 - returns correct response for canonical record`() {
        val prisonNumber = randomPrisonNumber()
        val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
          .copy(
            contacts = listOf(
              Contact(MOBILE, randomPhoneNumber()),
              Contact(EMAIL, randomEmail()),
            ),
            nationalities = listOf(randomNationalityCode()),
          )
        val person = createPersonWithNewKey(prisonPerson)

        val existingPrisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createPrisonReligionHistory()))

        val responseBody = sendGetRequestAsserted<DpsPrisonRecordTest>(
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
        val address = person.addresses.first()
        val canonicalAddress = CanonicalAddress(
          cprAddressId = address.updateId!!.toString(),
          noFixedAbode = address.noFixedAbode,
          startDate = address.startDate?.toUkLocalDate()?.toString(),
          startDateTime = address.startDate?.toUkLocalDateTime(),
          endDate = address.endDate?.toUkLocalDate()?.toString(),
          endDateTime = address.endDate?.toUkLocalDateTime(),
          postcode = address.postcode,
          buildingName = address.buildingName,
          buildingNumber = address.buildingNumber,
          thoroughfareName = address.thoroughfareName,
          dependentLocality = address.dependentLocality,
          postTown = address.postTown,
          county = address.county,
          country = address.countryCode?.description,
          countryCode = address.countryCode?.name,
          uprn = address.uprn,
          status = CanonicalAddressStatus.from(address.statusCode),
          comment = address.comment,
          usages = address.usages.map { CanonicalAddressUsage(CanonicalAddressUsageCode.from(it.usageCode), it.active) },
        )
        val address2 = person.addresses[1]
        val canonicalAddress2 = CanonicalAddress(
          cprAddressId = address2.updateId!!.toString(),
          noFixedAbode = address2.noFixedAbode,
          startDate = address2.startDate?.toUkLocalDate()?.toString(),
          startDateTime = address2.startDate?.toUkLocalDateTime(),
          endDate = address2.endDate?.toUkLocalDate()?.toString(),
          endDateTime = address2.endDate?.toUkLocalDateTime(),
          postcode = address2.postcode,
          buildingName = address2.buildingName,
          buildingNumber = address2.buildingNumber,
          thoroughfareName = address2.thoroughfareName,
          dependentLocality = address2.dependentLocality,
          postTown = address2.postTown,
          county = address2.county,
          country = address2.countryCode?.description,
          countryCode = address2.countryCode?.name,
          uprn = address2.uprn,
          status = CanonicalAddressStatus.from(address2.statusCode),
          comment = address2.comment,
          usages = address2.usages.map { CanonicalAddressUsage(CanonicalAddressUsageCode.from(it.usageCode), it.active) },
        )
        val canonicalContacts = person.contacts.map { CanonicalContact(cprContactId = it.updateId.toString(), type = CanonicalContactType.from(it.contactType), value = it.contactValue, extension = it.extension) }

        val canonicalReligion = CanonicalReligion(code = prisonPerson.religion?.name, description = prisonPerson.religion?.description)
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
        assertThat(responseBody.contacts).containsExactlyInAnyOrderElementsOf(canonicalContacts)

        assertThat(responseBody.addresses)
          .usingRecursiveComparison()
          .isEqualTo(listOf(canonicalAddress, canonicalAddress2))

        assertThat(responseBody.religionHistory.size).isEqualTo(1)
        assertThat(responseBody.religionHistory.first().startDate).isEqualTo(existingPrisonReligionEntity.startDate)
        assertThat(responseBody.religionHistory.first().endDate).isEqualTo(existingPrisonReligionEntity.endDate)
        assertThat(responseBody.religionHistory.first().religionCode).isEqualTo(existingPrisonReligionEntity.code)
        assertThat(responseBody.religionHistory.first().religionDescription).isEqualTo(existingPrisonReligionEntity.code.description)
        assertThat(responseBody.religionHistory.first().changeReasonKnown).isEqualTo(existingPrisonReligionEntity.changeReasonKnown)
        assertThat(responseBody.religionHistory.first().modifyDateTime).isEqualTo(existingPrisonReligionEntity.modifyDateTime)
        assertThat(responseBody.religionHistory.first().modifyUserId).isEqualTo(existingPrisonReligionEntity.modifyUserId)
        assertThat(responseBody.religionHistory.first().createDateTime).isEqualTo(existingPrisonReligionEntity.createDateTime)
        assertThat(responseBody.religionHistory.first().createUserId).isEqualTo(existingPrisonReligionEntity.createUserId)
        assertThat(responseBody.religionHistory.first().current).isEqualTo(existingPrisonReligionEntity.prisonRecordType.value)
        assertThat(responseBody.religionHistory.first().endDate).isEqualTo(existingPrisonReligionEntity.endDate)
        assertThat(responseBody.religionHistory.first().cprReligionId).isEqualTo(existingPrisonReligionEntity.updateId.toString())
      }

      @Test
      fun `should sort religions by start date and created date newest first`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber = prisonNumber))
        val now = LocalDate.now()
        val nowTime = LocalDateTime.now()

        sendPostRequestAsserted<Unit>(
          url = "/syscon-sync/person/$prisonNumber/religion",
          body = createPrisonReligionHistory().copy( // <- first in history to be written
            religionCode = BAHA,
            startDate = now.minusDays(1),
            endDate = now.minusDays(1),
            current = false,
            modifyDateTime = nowTime,
            createDateTime = nowTime.minusDays(1).minusHours(2),
          ),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = CREATED,
        )

        sendPostRequestAsserted<Unit>(
          url = "/syscon-sync/person/$prisonNumber/religion",
          body = createPrisonReligionHistory().copy( // <- second in history to be written
            religionCode = HUM,
            startDate = now.minusDays(1),
            endDate = now,
            current = false,
            modifyDateTime = nowTime,
            createDateTime = nowTime.minusDays(1).minusHours(1),
          ),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = CREATED,
        )

        sendPostRequestAsserted<Unit>(
          url = "/syscon-sync/person/$prisonNumber/religion",
          body = createPrisonReligionHistory().copy( // <- most recent in history to be written
            religionCode = AGNO,
            startDate = now,
            endDate = null,
            modifyDateTime = null,
            modifyUserId = null,
            current = true,
            createDateTime = nowTime,
          ),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = CREATED,
        )

        val responseBody = webTestClient.get()
          .uri(prisonApiUrl(prisonNumber))
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<DpsPrisonRecordTest>()
          .returnResult()
          .responseBody!!

        assertThat(responseBody.religionHistory.size).isEqualTo(3)
        assertThat(responseBody.religionHistory.first().current).isEqualTo(true)
        assertThat(responseBody.religionHistory.first().endDate).isNull()
        assertThat(responseBody.religionHistory.first().modifyDateTime).isNull()
        assertThat(responseBody.religionHistory.first().modifyUserId).isNull()

        assertThat(responseBody.religionHistory[1].current).isEqualTo(false)
        assertThat(responseBody.religionHistory[1].religionCode).isEqualTo(HUM)
        assertThat(responseBody.religionHistory[1].startDate).isEqualTo(now.minusDays(1))
        assertThat(responseBody.religionHistory[1].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(nowTime.minusDays(1).minusHours(1).truncatedTo(ChronoUnit.SECONDS))
        assertThat(responseBody.religionHistory[2].current).isEqualTo(false)
        assertThat(responseBody.religionHistory[2].religionCode).isEqualTo(BAHA)
        assertThat(responseBody.religionHistory[2].startDate).isEqualTo(now.minusDays(1))
        assertThat(responseBody.religionHistory[2].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(nowTime.minusDays(1).minusHours(2).truncatedTo(ChronoUnit.SECONDS))
      }

      @Test
      fun `should add list of additional identifiers to the prison record`() {
        val personOneCro = randomCro()
        val personTwoCro = randomCro()

        val personOneCrn = randomCrn()
        val personTwoCrn = randomCrn()

        val personOnePnc = randomLongPnc()
        val personTwoPnc = randomLongPnc()

        val personOneNationalInsuranceNumber = randomNationalInsuranceNumber()
        val personTwoNationalInsuranceNumber = randomNationalInsuranceNumber()

        val personOneArrestSummonNumber = randomArrestSummonsNumber()
        val personTwoArrestSummonNumber = randomArrestSummonsNumber()

        val personOneDriversLicenseNumber = randomDriverLicenseNumber()
        val personTwoDriversLicenseNumber = randomDriverLicenseNumber()

        val personOneDefendantId = randomDefendantId()
        val personTwoDefendantId = randomDefendantId()

        val personOne = Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = NOMIS,
          crn = personOneCrn,
          prisonNumber = randomPrisonNumber(),
          nationalities = listOf(randomNationalityCode()),
          cId = randomCId(),
          defendantId = personOneDefendantId,
          masterDefendantId = personOneDefendantId,
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
        )

        val personTwo = Person(
          firstName = randomName(),
          lastName = randomName(),
          middleNames = randomName(),
          dateOfBirth = randomDate(),
          sourceSystem = NOMIS,
          crn = personTwoCrn,
          prisonNumber = randomPrisonNumber(),
          nationalities = listOf(randomNationalityCode()),
          cId = randomCId(),
          defendantId = personTwoDefendantId,
          masterDefendantId = personTwoDefendantId,
          references = listOf(
            Reference(identifierType = CRO, identifierValue = personTwoCro),
            Reference(identifierType = PNC, identifierValue = personTwoPnc),
            Reference(
              identifierType = NATIONAL_INSURANCE_NUMBER,
              identifierValue = personTwoNationalInsuranceNumber,
            ),
            Reference(
              identifierType = ARREST_SUMMONS_NUMBER,
              identifierValue = personTwoArrestSummonNumber,
            ),
            Reference(
              identifierType = DRIVER_LICENSE_NUMBER,
              identifierValue = personTwoDriversLicenseNumber,
            ),
          ),
        )

        createPersonKey().addPerson(personOne).addPerson(personTwo)

        val responseBody = webTestClient.get()
          .uri(prisonApiUrl(personOne.prisonNumber))
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<DpsPrisonRecordTest>()
          .returnResult()
          .responseBody!!

        assertThat(responseBody.identifiers.cros).containsExactly(personOneCro)
        assertThat(responseBody.identifiers.pncs).containsExactly(personOnePnc)
        assertThat(responseBody.identifiers.nationalInsuranceNumbers).containsExactly(personOneNationalInsuranceNumber)
        assertThat(responseBody.identifiers.arrestSummonsNumbers).containsExactly(personOneArrestSummonNumber)
        assertThat(responseBody.identifiers.driverLicenseNumbers).containsExactly(personOneDriversLicenseNumber)
        assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrder(personOne.crn, personTwo.crn)
        assertThat(responseBody.identifiers.defendantIds).containsExactlyInAnyOrder(personOne.defendantId, personTwo.defendantId)
        assertThat(responseBody.identifiers.prisonNumbers).containsExactlyInAnyOrder(personOne.prisonNumber, personTwo.prisonNumber)
        assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrder(personOne.cId, personTwo.cId)
      }

      @Test
      fun `should return prison aliases with their identifiers for primary and non primary names`() {
        val prisonNumber = randomPrisonNumber()
        val person = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))

        val aliasPseudonym = savePseudonym(person, NameType.ALIAS)
        val aliasPrisonPseudonym = savePrisonPseudonym(aliasPseudonym, PrisonPseudonymSuffix.I, PrisonAliasNameType.CN)
        val primaryPseudonym = person.getPrimaryName()

        val primaryPnc = saveReference(person, primaryPseudonym, PNC, randomLongPnc())
        val aliasCro = saveReference(person, aliasPseudonym, CRO, randomCro(), comment = "alias cro", verified = true)
        val aliasNino = saveReference(person, aliasPseudonym, NATIONAL_INSURANCE_NUMBER, randomNationalInsuranceNumber(), verified = false)

        val responseBody = sendGetRequestAsserted<DpsPrisonRecordTest>(
          url = prisonApiUrl(prisonNumber),
          roles = listOf(API_READ_ONLY),
          expectedStatus = OK,
        ).returnResult().responseBody!!

        val primaryPseudonymResponse = responseBody.prisonAliases.single { it.isPrimary }
        assertThat(primaryPseudonymResponse.cprAliasId).isEqualTo(primaryPseudonym.updateId.toString())
        assertThat(primaryPseudonymResponse.firstName).isEqualTo(primaryPseudonym.firstName)
        assertThat(primaryPseudonymResponse.lastName).isEqualTo(primaryPseudonym.lastName)
        assertThat(primaryPseudonymResponse.dateOfBirth).isEqualTo(primaryPseudonym.dateOfBirth)
        assertThat(primaryPseudonymResponse.suffix).isNull()
        assertThat(primaryPseudonymResponse.nameType).isNull()
        assertThat(primaryPseudonymResponse.identifiers).hasSize(1)
        assertIdentifierMatches(primaryPseudonymResponse.identifiers.single(), primaryPnc)

        val aliasPseudonymResponse = responseBody.prisonAliases.single { it.cprAliasId == aliasPseudonym.updateId.toString() }
        assertThat(aliasPseudonymResponse.isPrimary).isFalse()
        assertThat(aliasPseudonymResponse.cprAliasId).isEqualTo(aliasPseudonym.updateId.toString())
        assertThat(aliasPseudonymResponse.titleCode).isEqualTo(aliasPseudonym.titleCode)
        assertThat(aliasPseudonymResponse.firstName).isEqualTo(aliasPseudonym.firstName)
        assertThat(aliasPseudonymResponse.middleNames).isEqualTo(aliasPseudonym.middleNames)
        assertThat(aliasPseudonymResponse.lastName).isEqualTo(aliasPseudonym.lastName)
        assertThat(aliasPseudonymResponse.dateOfBirth).isEqualTo(aliasPseudonym.dateOfBirth)
        assertThat(aliasPseudonymResponse.sexCode).isEqualTo(aliasPseudonym.sexCode)
        assertThat(aliasPseudonymResponse.ethnicity).isEqualTo(aliasPseudonym.ethnicityCode)
        assertThat(aliasPseudonymResponse.birthPlace).isEqualTo(aliasPseudonym.birthplace)
        assertThat(aliasPseudonymResponse.birthCountry).isEqualTo(aliasPseudonym.birthCountryCode)
        assertThat(aliasPseudonymResponse.suffix).isEqualTo(aliasPrisonPseudonym.suffix)
        assertThat(aliasPseudonymResponse.nameType).isEqualTo(aliasPrisonPseudonym.nameType)
        assertThat(aliasPseudonymResponse.createDateTime).isEqualTo(aliasPseudonym.createDateTime)
        assertThat(aliasPseudonymResponse.createUserId).isEqualTo(aliasPseudonym.createUserId)
        assertThat(aliasPseudonymResponse.modifyDateTime).isEqualTo(aliasPseudonym.modifyDateTime)
        assertThat(aliasPseudonymResponse.modifyUserId).isEqualTo(aliasPseudonym.modifyUserId)
        assertIdentifierMatches(aliasPseudonymResponse.identifiers.single { it.type == CRO }, aliasCro)
        assertIdentifierMatches(aliasPseudonymResponse.identifiers.single { it.type == NATIONAL_INSURANCE_NUMBER }, aliasNino)
      }

      @Test
      fun `should return redirect when the requested prison record has been merged`() {
        val sourcePrisonNumber = randomPrisonNumber()
        val targetPrisonNumber = randomPrisonNumber()

        val targetPersonEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(targetPrisonNumber))
        createMergedPerson(createRandomPrisonPersonDetails(sourcePrisonNumber), targetPersonEntity.id)

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

    private fun prisonApiUrl(prisonNumber: String?) = "/person/prison/dps/$prisonNumber"

    private fun savePseudonym(
      person: PersonEntity,
      nameType: NameType,
      titleCode: TitleCode? = randomTitleCode().value,
      firstName: String? = randomName(),
      middleNames: String? = randomName(),
      lastName: String? = randomName(),
      dateOfBirth: LocalDate? = randomDate(),
      sexCode: SexCode? = randomPrisonSexCode().value,
      ethnicityCode: EthnicityCode? = randomPrisonEthnicityCode(),
      birthplace: String? = "Leeds",
      birthCountryCode: CountryCode? = randomCountryCode(),
    ): PseudonymEntity = pseudonymRepository.saveAndFlush(
      PseudonymEntity(
        person = person,
        nameType = nameType,
        titleCode = titleCode,
        firstName = firstName,
        middleNames = middleNames,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        sexCode = sexCode,
        ethnicityCode = ethnicityCode,
        birthplace = birthplace,
        birthCountryCode = birthCountryCode,
        createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
        createUserId = "aliasCreateUser",
        modifyDateTime = LocalDateTime.of(2021, 2, 2, 13, 0),
        modifyUserId = "aliasModifyUser",
      ),
    )

    private fun savePrisonPseudonym(
      pseudonym: PseudonymEntity,
      suffix: PrisonPseudonymSuffix? = null,
      nameType: PrisonAliasNameType? = null,
    ): PrisonPseudonymEntity = prisonPseudonymRepository.saveAndFlush(
      PrisonPseudonymEntity(
        pseudonym = pseudonym,
        suffix = suffix,
        nameType = nameType,
      ),
    )

    private fun saveReference(
      person: PersonEntity,
      pseudonym: PseudonymEntity,
      type: IdentifierType,
      value: String,
      comment: String? = null,
      verified: Boolean? = null,
    ): ReferenceEntity = referenceRepository.saveAndFlush(
      ReferenceEntity(
        person = person,
        pseudonym = pseudonym,
        identifierType = type,
        identifierValue = value,
        comment = comment,
        isVerified = verified,
        createDateTime = LocalDateTime.of(2022, 3, 3, 14, 0),
        createUserId = "identifierCreateUser",
        modifyDateTime = LocalDateTime.of(2023, 4, 4, 15, 0),
        modifyUserId = "identifierModifyUser",
      ),
    )

    private fun assertIdentifierMatches(actual: DpsPrisonIdentifier, expected: ReferenceEntity) {
      assertThat(actual.cprIdentifierId).isEqualTo(expected.updateId.toString())
      assertThat(actual.type).isEqualTo(expected.identifierType)
      assertThat(actual.value).isEqualTo(expected.identifierValue)
      assertThat(actual.comment).isEqualTo(expected.comment)
      assertThat(actual.verified).isEqualTo(expected.isVerified)
      assertThat(actual.createDateTime).isEqualTo(expected.createDateTime)
      assertThat(actual.createUserId).isEqualTo(expected.createUserId)
      assertThat(actual.modifyDateTime).isEqualTo(expected.modifyDateTime)
      assertThat(actual.modifyUserId).isEqualTo(expected.modifyUserId)
    }
  }
}

// JsonUnwrapped annotation on DpsPrisonRecord produces this structure, so we cannot use DpsPrisonRecord directly to mimic return value
// this is a copy/paste of CanonicalRecord with the additional properties of DpsPrisonRecord
data class DpsPrisonRecordTest(
  val cprUUID: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: String? = null,
  val disability: Boolean? = null,
  val interestToImmigration: Boolean? = null,
  val title: CanonicalTitle,
  val sex: CanonicalSex,
  val sexualOrientation: CanonicalSexualOrientation,
  val religion: CanonicalReligion,
  val ethnicity: CanonicalEthnicity,
  val aliases: List<CanonicalAlias> = emptyList(),
  var nationalities: List<CanonicalNationality> = emptyList(),
  val addresses: List<CanonicalAddress> = emptyList(),
  val identifiers: CanonicalIdentifiers,
  val contacts: List<CanonicalContact> = emptyList(),
  val religionHistory: List<PrisonReligion>,
  val prisonAliases: List<DpsPrisonAlias> = emptyList(),
)
