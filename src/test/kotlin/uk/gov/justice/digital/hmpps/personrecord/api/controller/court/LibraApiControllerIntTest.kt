package uk.gov.justice.digital.hmpps.personrecord.api.controller.court

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode

class LibraApiControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return when all fields populated`() {
      val firstName = randomName()
      val lastName = randomName()
      val middleNames = randomName()
      val title = randomTitleCode()
      val pnc = randomLongPnc()
      val noFixedAbode = true
      val startDate = randomDate()
      val endDate = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDate()
      val postcode = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode()
      val nationality = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode()
      val religion = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomReligion()
      val ethnicity = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity()
      val sex = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformSexCode()

      val buildingName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName()
      val buildingNumber = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber()
      val thoroughfareName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName()
      val dependentLocality = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName()
      val postTown = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName()

      val cro = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCro()
      val crn = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCrn()
      val defendantId = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId()
      val prisonNumber = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber()
      val cid = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCId()

      val person = createPersonWithNewKey(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Person(
          firstName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          lastName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          middleNames = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          dateOfBirth = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDate(),
          sourceSystem = uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA,
          titleCode = title.value,
          crn = crn,
          sexCode = sex.value,
          prisonNumber = prisonNumber,
          nationalities = listOf(nationality),
          religion = religion,
          cId = cid,
          ethnicityCode = uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.Companion.fromCommonPlatform(
            ethnicity,
          ),
          defendantId = defendantId,
          aliases = listOf(
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Alias(
              firstName = firstName,
              middleNames = middleNames,
              lastName = lastName,
              dateOfBirth = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDate(),
              titleCode = title.value,
              sexCode = sex.value,
            ),
          ),
          addresses = listOf(
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Address(
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
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC,
              identifierValue = pnc,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO,
              identifierValue = cro,
            ),
          ),
        ),
      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!
      val canonicalAlias =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias(
          firstName = firstName,
          lastName = lastName,
          middleNames = middleNames,
          title = uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle.Companion.from(title.value),
          sex = uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex.Companion.from(sex.value),
        )
      val canonicalNationality = listOf(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality(
          nationality.name,
          nationality.description,
        ),
      )
      val canonicalAddress =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress(
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
      val canonicalReligion =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion(
          code = religion,
          description = religion,
        )
      val canonicalEthnicity = uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity.Companion.from(
        uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.Companion.fromCommonPlatform(ethnicity),
      )
      org.assertj.core.api.Assertions.assertThat(responseBody.cprUUID).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.firstName).isEqualTo(person.getPrimaryName().firstName)
      org.assertj.core.api.Assertions.assertThat(responseBody.middleNames).isEqualTo(person.getPrimaryName().middleNames)
      org.assertj.core.api.Assertions.assertThat(responseBody.lastName).isEqualTo(person.getPrimaryName().lastName)
      org.assertj.core.api.Assertions.assertThat(responseBody.dateOfBirth).isEqualTo(person.getPrimaryName().dateOfBirth.toString())
      org.assertj.core.api.Assertions.assertThat(responseBody.title.code).isEqualTo(person.getPrimaryName().titleCode?.name)
      org.assertj.core.api.Assertions.assertThat(responseBody.title.description).isEqualTo(person.getPrimaryName().titleCode?.description)
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().title.code).isEqualTo(person.getAliases().first().titleCode?.name)
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().title.description).isEqualTo(
        person.getAliases().first().titleCode?.description,
      )
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().sex.code).isEqualTo(person.getAliases().first().sexCode?.name)
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().sex.description).isEqualTo(person.getAliases().first().sexCode?.description)
      org.assertj.core.api.Assertions.assertThat(responseBody.nationalities.first().code).isEqualTo(canonicalNationality.first().code)
      org.assertj.core.api.Assertions.assertThat(responseBody.nationalities.first().description).isEqualTo(canonicalNationality.first().description)
      org.assertj.core.api.Assertions.assertThat(responseBody.sex.code).isEqualTo(sex.value.name)
      org.assertj.core.api.Assertions.assertThat(responseBody.sex.description).isEqualTo(sex.value.description)
      org.assertj.core.api.Assertions.assertThat(responseBody.religion.code).isEqualTo(canonicalReligion.code)
      org.assertj.core.api.Assertions.assertThat(responseBody.religion.description).isEqualTo(canonicalReligion.description)
      org.assertj.core.api.Assertions.assertThat(responseBody.ethnicity.code).isEqualTo(canonicalEthnicity.code)
      org.assertj.core.api.Assertions.assertThat(responseBody.ethnicity.description).isEqualTo(canonicalEthnicity.description)
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases).isEqualTo(listOf(canonicalAlias))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.cros).isEqualTo(listOf(cro))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.pncs).isEqualTo(listOf(pnc))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.crns).isEqualTo(listOf(crn))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.defendantIds).isEqualTo(listOf(defendantId))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.prisonNumbers).isEqualTo(listOf(prisonNumber))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cid))
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses).isEqualTo(listOf(canonicalAddress))
    }

    @org.junit.jupiter.api.Test
    fun `should return nulls when values are null or empty`() {
      val cId = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCId()

      val person = createPersonWithNewKey(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Person(
          sourceSystem = uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA,
          cId = cId,
        ),

      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      org.assertj.core.api.Assertions.assertThat(responseBody.firstName).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.middleNames).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.lastName).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.dateOfBirth).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.title.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.title.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.ethnicity.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.ethnicity.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.sex.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.sex.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.religion.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.religion.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.title.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.title.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.ethnicity.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.ethnicity.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.sex.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.sex.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.religion.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.religion.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.nationalities).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.crns).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.defendantIds).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.cids).isEqualTo(listOf(cId))
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.prisonNumbers).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.pncs).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.cros).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.nationalInsuranceNumbers).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.driverLicenseNumbers).isEmpty()
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.arrestSummonsNumbers).isEmpty()
    }

    @org.junit.jupiter.api.Test
    fun `should return nulls when some data (eg alias and address) values are null or empty`() {
      val cId = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCId()

      val aliasFirstName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName()
      val postcode = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode()
      val person = createPersonWithNewKey(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Person(
          sourceSystem = uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA,
          cId = cId,
          aliases = listOf(_root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Alias(firstName = aliasFirstName)),
          addresses = listOf(
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Address(
              postcode = postcode,
            ),
          ),
        ),

      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(person.cId))
        .authorised(listOf(uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().firstName).isEqualTo(aliasFirstName)
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().lastName).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().middleNames).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().title.code).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.aliases.first().title.description).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().postcode).isEqualTo(postcode)
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().startDate).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().endDate).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().noFixedAbode).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().buildingName).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().buildingNumber).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().thoroughfareName).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().dependentLocality).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().postTown).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().county).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().country).isNull()
      org.assertj.core.api.Assertions.assertThat(responseBody.addresses.first().uprn).isNull()
    }

    @org.junit.jupiter.api.Test
    fun `should add list of additional identifiers off two people`() {
      val personOneCro = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCro()
      val personTwoCro = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCro()

      val personTwoCrn = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCrn()

      val personOnePnc = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc()
      val personTwoPnc = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc()

      val personOneNationalInsuranceNumber =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber()
      val personTwoNationalInsuranceNumber =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber()

      val personOneArrestSummonNumber =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber()
      val personTwoArrestSummonNumber =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomArrestSummonNumber()

      val personOneDriversLicenseNumber =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber()
      val personTwoDriversLicenseNumber =
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber()

      val personOneCId = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCId()

      val personOne = createPerson(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Person(
          firstName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          lastName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          middleNames = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          dateOfBirth = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDate(),
          sourceSystem = uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA,
          nationalities = listOf(_root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode()),
          religion = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomReligion(),

          cId = personOneCId,
          references = listOf(
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO,
              identifierValue = personOneCro,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC,
              identifierValue = personOnePnc,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER,
              identifierValue = personOneNationalInsuranceNumber,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER,
              identifierValue = personOneArrestSummonNumber,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER,
              identifierValue = personOneDriversLicenseNumber,
            ),
          ),
        ),
      )

      val personTwo = createPerson(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Person(
          firstName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          lastName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          middleNames = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          dateOfBirth = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDate(),
          sourceSystem = uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS,
          crn = personTwoCrn,
          nationalities = listOf(_root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode()),
          religion = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomReligion(),
          references = listOf(
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO,
              identifierValue = personTwoCro,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC,
              identifierValue = personTwoPnc,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER,
              identifierValue = personTwoNationalInsuranceNumber,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER,
              identifierValue = personTwoArrestSummonNumber,
            ),
            _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Reference(
              identifierType = uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER,
              identifierValue = personTwoDriversLicenseNumber,
            ),
          ),
        ),
      )
      createPersonKey().addPerson(personOne).addPerson(personTwo)

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(personOne.cId))
        .authorised(listOf(uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.cros).containsExactly(personOneCro)
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.pncs).containsExactly(personOnePnc)
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.nationalInsuranceNumbers).containsExactly(personOneNationalInsuranceNumber)
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.arrestSummonsNumbers).containsExactly(personOneArrestSummonNumber)
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.driverLicenseNumbers).containsExactly(personOneDriversLicenseNumber)
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.crns).containsExactlyInAnyOrderElementsOf(
        listOf(
          personTwo.crn,
        ),
      )
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.cids).containsExactlyInAnyOrderElementsOf(
        listOf(
          personOne.cId,
        ),
      )
    }

    @org.junit.jupiter.api.Test
    fun `should return linked crn`() {
      val cId = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCId()
      val crn = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomCrn()

      val person = createPersonWithNewKey(
        _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.model.person.Person(
          firstName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          lastName = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          middleNames = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomName(),
          dateOfBirth = _root_ide_package_.uk.gov.justice.digital.hmpps.personrecord.test.randomDate(),
          sourceSystem = uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA,
          crn = crn,
          cId = cId,
        ),
      )

      val responseBody = webTestClient.get()
        .uri(libraApiUrl(cId))
        .authorised(listOf(uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      println("responseBody = $responseBody")

      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.crns.size).isEqualTo(1)
      org.assertj.core.api.Assertions.assertThat(responseBody.identifiers.crns.first()).isEqualTo(crn)
    }
  }

  @org.junit.jupiter.api.Nested
  inner class ErrorScenarios {

    @org.junit.jupiter.api.Test
    fun `should return not found 404 with userMessage to show that the c_Id is not found`() {
      val nonExistentCId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
      val expectedErrorMessage = "Not found: $nonExistentCId"
      webTestClient.get()
        .uri(libraApiUrl(nonExistentCId))
        .authorised(listOf(uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @org.junit.jupiter.api.Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(libraApiUrl("accessdenied"))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @org.junit.jupiter.api.Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.get()
        .uri(libraApiUrl("unauthorised"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun libraApiUrl(cId: String?) = "/person/libra/$cId"
}
