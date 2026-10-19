package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionInsertRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.HUM
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonAPIReligionControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  @DisplayName("GET /person/prison/{prisonNumber}/religion-history")
  inner class GetReligionHistoryByPrisonNumber {
    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should return prison religion history for a prison number`() {
        val prisonPerson = createRandomPrisonPersonDetails()
          .copy(
            contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")),
            nationalities = listOf(randomNationalityCode()),
          )
        createPersonWithNewKey(prisonPerson)
        val prisonNumber = prisonPerson.prisonNumber!!
        val existingPrisonReligionEntity =
          prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createPrisonReligionHistory()))

        val responseBody = webTestClient.get()
          .uri(prisonApiUrl(prisonNumber))
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<PrisonReligion>>()
          .returnResult()
          .responseBody!!

        assertThat(responseBody.size).isEqualTo(1)
        assertThat(responseBody.first().startDate).isEqualTo(existingPrisonReligionEntity.startDate)
        assertThat(responseBody.first().endDate).isEqualTo(existingPrisonReligionEntity.endDate)
        assertThat(responseBody.first().religionCode).isEqualTo(existingPrisonReligionEntity.code)
        assertThat(responseBody.first().religionDescription).isEqualTo(existingPrisonReligionEntity.code.description)
        assertThat(responseBody.first().changeReasonKnown).isEqualTo(existingPrisonReligionEntity.changeReasonKnown)
        assertThat(responseBody.first().modifyDateTime).isEqualTo(existingPrisonReligionEntity.modifyDateTime)
        assertThat(responseBody.first().modifyUserId).isEqualTo(existingPrisonReligionEntity.modifyUserId)
        assertThat(responseBody.first().createDateTime).isEqualTo(existingPrisonReligionEntity.createDateTime)
        assertThat(responseBody.first().createUserId).isEqualTo(existingPrisonReligionEntity.createUserId)
        assertThat(responseBody.first().current).isEqualTo(existingPrisonReligionEntity.prisonRecordType.value)
        assertThat(responseBody.first().endDate).isEqualTo(existingPrisonReligionEntity.endDate)
        assertThat(responseBody.first().cprReligionId).isEqualTo(existingPrisonReligionEntity.updateId.toString())
      }

      @Test
      fun `should sort religions by start date and created date newest first`() {
        val prisonNumber = randomPrisonNumber()
        val person = createRandomPrisonPersonDetails(prisonNumber = prisonNumber)
        createPersonWithNewKey(person)
        val now = LocalDate.now()
        val nowTime = LocalDateTime.now()

        sendPostRequestAsserted<Unit>(
          url = "/syscon-sync/person/$prisonNumber/religion",
          body = createPrisonReligionHistory().copy(
            // <- first in history to be written
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
          body = createPrisonReligionHistory().copy(
            // <- second in history to be written
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
          body = createPrisonReligionHistory().copy(
            // <- most recent in history to be written
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
          .expectBody<List<PrisonReligion>>()
          .returnResult()
          .responseBody!!

        assertThat(responseBody.size).isEqualTo(3)
        assertThat(responseBody.first().current).isEqualTo(true)
        assertThat(responseBody.first().endDate).isNull()
        assertThat(responseBody.first().modifyDateTime).isNull()
        assertThat(responseBody.first().modifyUserId).isNull()

        assertThat(responseBody[1].current).isEqualTo(false)
        assertThat(responseBody[1].religionCode).isEqualTo(HUM)
        assertThat(responseBody[1].startDate).isEqualTo(now.minusDays(1))
        assertThat(responseBody[1].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
          nowTime.minusDays(1).minusHours(1).truncatedTo(ChronoUnit.SECONDS),
        )
        assertThat(responseBody[2].current).isEqualTo(false)
        assertThat(responseBody[2].religionCode).isEqualTo(BAHA)
        assertThat(responseBody[2].startDate).isEqualTo(now.minusDays(1))
        assertThat(responseBody[2].createDateTime.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(
          nowTime.minusDays(1).minusHours(2).truncatedTo(ChronoUnit.SECONDS),
        )
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

    private fun prisonApiUrl(prisonNumber: String?) = "/person/prison/$prisonNumber/religion-history"
  }

  @Nested
  @DisplayName("PUT /person/prison/{prisonNumber}/religion")
  inner class UpdateReligionByPrisonNumber {
    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should insert the new religion and update the person's current religion`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val request = PrisonReligionInsertRequest(religionCode = BAHA, comment = "Requested update", userId = "TEST")

        sendPutRequestAsserted<Unit>(
          url = religionApiUrl(prisonNumber),
          body = request,
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NO_CONTENT,
        )

        val actualReligion = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber).single()
        assertThat(actualReligion.code).isEqualTo(request.religionCode)
        assertThat(actualReligion.comments).isEqualTo(request.comment)
        assertThat(actualReligion.changeReasonKnown).isTrue()
        assertThat(actualReligion.prisonRecordType.value).isTrue()
        assertThat(actualReligion.createUserId).isEqualTo(request.userId)
        assertThat(personRepository.findByPrisonNumber(prisonNumber)?.religion).isEqualTo(request.religionCode)
      }

      @Test
      fun `should make the existing current religion historic when inserting a new religion`() {
        val prisonNumber = randomPrisonNumber()
        val existingReligion = PrisonReligionEntity.from(
          prisonNumber,
          createPrisonReligionHistory(code = HUM).copy(endDate = null),
        )
        val person = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber), configure = { religion = existingReligion.code })
        prisonReligionRepository.save(existingReligion)
        personRepository.saveAndFlush(person)
        val request = PrisonReligionInsertRequest(religionCode = BAHA, comment = "Requested update", userId = "TEST")

        sendPutRequestAsserted<Unit>(
          url = religionApiUrl(prisonNumber),
          body = request,
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NO_CONTENT,
        )

        val actualReligions = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)
        assertThat(actualReligions).hasSize(2)
        val actualExistingReligion = actualReligions.single { it.code == HUM }
        assertThat(actualExistingReligion.prisonRecordType.value).isFalse()
        assertThat(actualExistingReligion.endDate).isEqualTo(LocalDate.now())
        assertThat(actualExistingReligion.modifyUserId).isEqualTo(request.userId)
        assertThat(actualExistingReligion.modifyDateTime).isNotNull()

        val actualNewReligion = actualReligions.single { it.code == BAHA }
        assertThat(actualNewReligion.prisonRecordType.value).isTrue()
        assertThat(actualNewReligion.endDate).isNull()
        assertThat(personRepository.findByPrisonNumber(prisonNumber)?.religion).isEqualTo(request.religionCode)
      }
    }

    @Nested
    inner class ErrorScenarios {
      @Test
      fun `should return not found when the prison number does not exist`() {
        sendPutRequestAsserted<Unit>(
          url = religionApiUrl(randomPrisonNumber()),
          body = PrisonReligionInsertRequest(religionCode = BAHA, userId = "TEST"),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = HttpStatus.NOT_FOUND,
        )
      }

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<Unit>(
          url = religionApiUrl(randomPrisonNumber()),
          body = PrisonReligionInsertRequest(religionCode = BAHA, userId = "TEST"),
          roles = listOf(API_READ_ONLY),
          expectedStatus = HttpStatus.FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = religionApiUrl(randomPrisonNumber()),
          body = PrisonReligionInsertRequest(religionCode = BAHA, userId = "TEST"),
          roles = emptyList(),
          expectedStatus = HttpStatus.UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    private fun religionApiUrl(prisonNumber: String) = "/person/prison/$prisonNumber/religion"
  }

  @Nested
  @DisplayName("GET /person/prison/religion-codes")
  inner class GetReligionCodes {
    @Nested
    inner class SuccessfulProcessing {
      @Test
      fun `should return only active prison religion codes by default`() {
        val responseBody = webTestClient.get()
          .uri(religionCodesApiUrl())
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<ReferenceDataResponse>>()
          .returnResult()
          .responseBody!!

        val expectedReligionCodes = ReligionCode.entries
          .filter { it.current }
          .map { ReferenceDataResponse(it.name, it.description, it.current) }
        assertThat(responseBody).isEqualTo(expectedReligionCodes)
      }

      @Test
      fun `should return all religion codes when active is false`() {
        val responseBody = webTestClient.get()
          .uri("${religionCodesApiUrl()}?active=false")
          .authorised(listOf(API_READ_ONLY))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<List<ReferenceDataResponse>>()
          .returnResult()
          .responseBody!!

        val expectedReligionCodes = ReligionCode.entries
          .map { ReferenceDataResponse(it.name, it.description, it.current) }
        assertThat(responseBody).isEqualTo(expectedReligionCodes)
      }
    }

    @Nested
    inner class ErrorScenarios {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.get()
          .uri(religionCodesApiUrl())
          .authorised(listOf("UNSUPPORTED-ROLE"))
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        webTestClient.get()
          .uri(religionCodesApiUrl())
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    private fun religionCodesApiUrl() = "/person/prison/religion-codes"
  }
}
