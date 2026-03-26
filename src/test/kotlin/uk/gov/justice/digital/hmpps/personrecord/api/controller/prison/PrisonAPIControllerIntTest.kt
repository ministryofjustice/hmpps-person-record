package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.Identifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionGet
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.HUM
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Autowired
  private lateinit var prisonReferenceRepository: PrisonReferenceRepository

  // TODO: once POST /person/prison endpoint is ready, use it here instead of manually setting up data?
  @Nested
  inner class CanonicalBehaviour {

    @Test
    fun `cluster size 1 - returns correct response for canonical record`() {
      val prisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
        .copy(
          references = listOf(Reference(identifierType = PNC, identifierValue = randomLongPnc(), comment = randomLowerCaseString())),
          contacts = listOf(Contact(ContactType.MOBILE, randomPhoneNumber(), "+44")),
        )
      val cluster = createPersonKey()
        .addPerson(prisonPerson)

      val actualPeronEntity = cluster.personEntities.first()
      val actualResponseBody = sendGetRequestAsserted<PrisonCanonicalRecord>(
        url = prisonApiUrl(prisonNumber),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      assertThat(actualResponseBody.record).usingRecursiveComparison().isEqualTo(CanonicalRecord.from(actualPeronEntity))
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
          contacts = listOf(Contact(ContactType.MOBILE, randomPhoneNumber(), "+44")),
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
      val actualResponseBody = sendGetRequestAsserted<PrisonCanonicalRecord>(
        url = prisonApiUrl(prisonNumber2),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      val expectedCanonicalRecord = CanonicalRecord.from(actualNewestPeronEntity)
      assertThat(actualResponseBody.record).usingRecursiveComparison().isEqualTo(expectedCanonicalRecord)

      assertThat(actualResponseBody.record.identifiers.cros).containsExactly(prisonPerson2.getCro())
      assertThat(actualResponseBody.record.identifiers.pncs).containsExactly(prisonPerson2.getPnc())
      assertThat(actualResponseBody.record.identifiers.nationalInsuranceNumbers).containsExactly(
        prisonPerson2.references.filter { it.identifierType == NATIONAL_INSURANCE_NUMBER }.map { it.identifierValue }.first(),
      )
      assertThat(actualResponseBody.record.identifiers.arrestSummonsNumbers).containsExactly(
        prisonPerson2.references.filter { it.identifierType == ARREST_SUMMONS_NUMBER }.map { it.identifierValue }.first(),
      )
      assertThat(actualResponseBody.record.identifiers.driverLicenseNumbers).containsExactly(
        prisonPerson2.references.filter { it.identifierType == DRIVER_LICENSE_NUMBER }.map { it.identifierValue }.first(),
      )
      assertThat(actualResponseBody.record.identifiers.crns).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.crn })
      assertThat(actualResponseBody.record.identifiers.defendantIds).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.defendantId })
      assertThat(actualResponseBody.record.identifiers.prisonNumbers).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.prisonNumber })
      assertThat(actualResponseBody.record.identifiers.cids).containsExactlyInAnyOrderElementsOf(cluster.personEntities.map { it.cId })
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
  inner class PrisonSpecificBehaviour {
    @Test
    fun `prison religion data exists - returns correct response for religion history`() {
      val prisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
        .copy(
          references = listOf(Reference(identifierType = PNC, identifierValue = randomLongPnc(), comment = randomLowerCaseString())),
          contacts = listOf(Contact(ContactType.MOBILE, randomPhoneNumber(), "+44")),
        )
      createPersonKey()
        .addPerson(prisonPerson)
        .also { prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion())) }

      val actualPrisonReligionEntity = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber).first()
      val actualResponseBody = sendGetRequestAsserted<PrisonCanonicalRecord>(
        url = prisonApiUrl(prisonNumber),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      assertThat(actualResponseBody.religionHistory).usingRecursiveComparison().isEqualTo(listOf(actualPrisonReligionEntity).map { PrisonReligionGet.from(it) })
    }

    @Test
    fun `prison specific references exist - returns correct response for prison references`() {
      val prisonNumber = randomPrisonNumber()
      val prisonPerson = createRandomPrisonPersonDetails(prisonNumber)
        .copy(
          references = listOf(Reference(identifierType = PNC, identifierValue = randomLongPnc(), comment = randomLowerCaseString())),
          contacts = listOf(Contact(ContactType.MOBILE, randomPhoneNumber(), "+44")),
        )
      val cluster = createPersonKey()
        .addPerson(prisonPerson)
        .also { cluster ->
          val personEntity = cluster.personEntities.first()
          personEntity.pseudonyms.forEach { pseudonymEntity ->
            val personsReference = personEntity.references.first()
            val prisonReferenceEntity = PrisonReferenceEntity.from(
              Reference(identifierType = personsReference.identifierType, identifierValue = personsReference.identifierValue, comment = personsReference.comment),
            )
            prisonReferenceEntity.pseudonym = pseudonymEntity
            prisonReferenceRepository.save(prisonReferenceEntity)
          }
        }

      val actualPeronEntity = cluster.personEntities.first()
      val actualResponseBody = sendGetRequestAsserted<PrisonCanonicalRecord>(
        url = prisonApiUrl(prisonNumber),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      val expectedPrisonAliases = actualPeronEntity.pseudonyms
        .map { pseudonymEntity ->
          val referencesForPseudonym = prisonReferenceRepository.findAllByPseudonym(pseudonymEntity)
          PrisonAlias(
            titleCode = pseudonymEntity.titleCode,
            firstName = pseudonymEntity.firstName,
            middleNames = pseudonymEntity.middleNames,
            lastName = pseudonymEntity.lastName,
            dateOfBirth = pseudonymEntity.dateOfBirth,
            sexCode = pseudonymEntity.sexCode,
            isPrimary = pseudonymEntity.nameType == NameType.PRIMARY,
            identifiers = referencesForPseudonym.map {
              Identifier(
                type = it.identifierType,
                value = it.identifierValue,
                comment = it.comment,
              )
            },
          )
        }

      assertThat(actualResponseBody.prisonAliases).usingRecursiveComparison().isEqualTo(expectedPrisonAliases)
    }

    @Test
    fun `should sort religions by start date and created date newest first`() {
      val prisonNumber = randomPrisonNumber()
      val person = createPerson(createRandomPrisonPersonDetails(prisonNumber = prisonNumber))
      createPersonKey()
        .addPerson(person)
      val now = LocalDate.now()
      val nowTime = LocalDateTime.now()

      sendPostRequestAsserted<Unit>(
        url = "/person/prison/$prisonNumber/religion",
        body = createRandomReligion().copy( // <- first in history to be written
          religionCode = BAHA.name,
          startDate = now.minusDays(1),
          endDate = now.minusDays(1),
          current = false,
          modifyDateTime = nowTime,
          createDateTime = nowTime.minusDays(1).minusHours(2),
        ),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      sendPostRequestAsserted<Unit>(
        url = "/person/prison/$prisonNumber/religion",
        body = createRandomReligion().copy( // <- second in history to be written
          religionCode = HUM.name,
          startDate = now.minusDays(1),
          endDate = now,
          current = false,
          modifyDateTime = nowTime,
          createDateTime = nowTime.minusDays(1).minusHours(1),
        ),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      sendPostRequestAsserted<Unit>(
        url = "/person/prison/$prisonNumber/religion",
        body = createRandomReligion().copy( // <- most recent in history to be written
          religionCode = AGNO.name,
          startDate = now,
          endDate = null,
          modifyDateTime = null,
          modifyUserId = null,
          current = true,
          createDateTime = nowTime,
        ),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      val responseBody = webTestClient.get()
        .uri(prisonApiUrl(prisonNumber))
        .authorised(listOf(API_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(PrisonCanonicalRecord::class.java)
        .returnResult()
        .responseBody!!

      assertThat(responseBody.religionHistory.size).isEqualTo(3)
      assertThat(responseBody.religionHistory.first().current).isEqualTo(true)
      assertThat(responseBody.religionHistory.first().endDate).isNull()
      assertThat(responseBody.religionHistory.first().modifyDateTime).isNull()
      assertThat(responseBody.religionHistory.first().modifyUserId).isNull()

      assertThat(responseBody.religionHistory[1].current).isEqualTo(false)
      assertThat(responseBody.religionHistory[1].religionCode).isEqualTo(HUM.name)
      assertThat(responseBody.religionHistory[1].startDate).isEqualTo(now.minusDays(1))
      assertThat(responseBody.religionHistory[1].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(nowTime.minusDays(1).minusHours(1).truncatedTo(ChronoUnit.SECONDS))
      assertThat(responseBody.religionHistory[2].current).isEqualTo(false)
      assertThat(responseBody.religionHistory[2].religionCode).isEqualTo(BAHA.name)
      assertThat(responseBody.religionHistory[2].startDate).isEqualTo(now.minusDays(1))
      assertThat(responseBody.religionHistory[2].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(nowTime.minusDays(1).minusHours(2).truncatedTo(ChronoUnit.SECONDS))
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
