package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.religion

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonReligionPostAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Successful {

    @Test
    fun `person has no prison religions - saves prison religion - updates current religion`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion()
      sendPostRequestAsserted<PrisonReligionResponseBody>(
        url = prisonReligionPostEndpoint(prisonNumber),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(requestBody.religionCode)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
        assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligionEntities.first(), requestBody)
      }
    }

    @Test
    fun `person has existing current prison religion - does not save prison religion - return internal server error`() {
      val prisonNumber = randomPrisonNumber()
      val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createRandomReligion())
      val personEntityWithCurrentReligion = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingReligionEntity.code))
      prisonReligionRepository.save(existingReligionEntity)
      personRepository.saveAndFlush(personEntityWithCurrentReligion)

      val requestBody = createRandomReligion()
      sendPostRequestAsserted<Unit>(
        url = prisonReligionPostEndpoint(prisonNumber),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR,
      )

      awaitAssert {
        val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(actualPersonEntity.religion).isEqualTo(actualPersonEntity.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
      }
    }

    @Test
    fun `saves prison religion - returns correct response body`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion()
      val responseBody = sendPostRequestAsserted<PrisonReligionResponseBody>(
        url = prisonReligionPostEndpoint(prisonNumber),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      awaitAssert {
        val actualPrisonReligionEntity = prisonReligionRepository.findByPrisonNumber(prisonNumber).first()
        val expectedResponseBody = PrisonReligionResponseBody(prisonNumber, PrisonReligionMapping(requestBody.nomisReligionId, actualPrisonReligionEntity.updateId.toString()))
        responseBody.isEqualTo(expectedResponseBody)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `person does not exist - returns 404 not found`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion()
      sendPostRequestAsserted<Unit>(
        url = prisonReligionPostEndpoint(randomPrisonNumber()),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(null)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(0)
      }
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = prisonReligionPostEndpoint(randomPrisonNumber()),
        body = createRandomReligion(),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = prisonReligionPostEndpoint(randomPrisonNumber()),
        body = createRandomReligion(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun assertPrisonReligionEntityColumns(
    prisonNumber: String,
    actual: PrisonReligionEntity,
    expected: PrisonReligion,
  ) {
    assertThat(actual.updateId.toString()).isNotEmpty()
    assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
    assertThat(actual.changeReasonKnown).isEqualTo(expected.changeReasonKnown)
    assertThat(actual.comments).isEqualTo(expected.comments)
    assertThat(actual.verified).isEqualTo(expected.verified)
    assertThat(actual.startDate).isEqualTo(expected.startDate)
    assertThat(actual.endDate).isEqualTo(expected.endDate)
    assertThat(actual.modifyDateTime).isEqualTo(expected.modifyDateTime)
    assertThat(actual.prisonRecordType).isEqualTo(PrisonRecordType.from(expected.current))
  }

  private fun prisonReligionPostEndpoint(prisonNumber: String) = "/person/prison/$prisonNumber/religion"
}
