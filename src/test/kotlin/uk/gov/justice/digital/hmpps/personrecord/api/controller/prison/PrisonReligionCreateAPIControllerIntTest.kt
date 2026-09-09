package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionInsertRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import java.time.LocalDate

class PrisonReligionCreateAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Successful {

    @Test
    fun `person has no prison religions - saves prison religion - updates current religion`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligionInsertRequest()
      sendPostRequestAsserted<Unit>(
        url = prisonReligionCreateEndpoint(prisonNumber),
        body = requestBody,
        roles = listOf(PRISON_API_READ_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(requestBody.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
        assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligionEntities.first(), requestBody)
      }
    }

    @Test
    fun `person has existing current prison religion - saves new prison religion - updates current religion`() {
      val prisonNumber = randomPrisonNumber()
      val existingReligionCode = randomReligionCode()
      val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createPrisonReligionHistory(code = existingReligionCode))
      val personEntityWithCurrentReligion = createPerson(createRandomPrisonPersonDetails(prisonNumber), configure = { religion = existingReligionEntity.code })
      prisonReligionRepository.save(existingReligionEntity)
      personRepository.saveAndFlush(personEntityWithCurrentReligion)

      val requestBody = createRandomReligionInsertRequest(existingReligionEntity.code)
      sendPostRequestAsserted<Unit>(
        url = prisonReligionCreateEndpoint(prisonNumber),
        body = requestBody,
        roles = listOf(PRISON_API_READ_WRITE),
        expectedStatus = HttpStatus.CREATED,
      )

      awaitAssert {
        val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(actualPersonEntity.religion).isEqualTo(requestBody.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(2)
        val actualPrisonReligion = actualPrisonReligionEntities.first()
        assertThat(actualPrisonReligion.prisonRecordType).isEqualTo(PrisonRecordType.CURRENT)
        assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligion, requestBody)

        val actualPreviousPrisonReligion = actualPrisonReligionEntities[1]
        assertThat(actualPreviousPrisonReligion.prisonRecordType).isEqualTo(PrisonRecordType.HISTORIC)
        assertThat(actualPreviousPrisonReligion.endDate).isEqualTo(LocalDate.now())
        assertThat(actualPreviousPrisonReligion.modifyUserId).isEqualTo(requestBody.userId)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `person does not exist - returns 404 not found`() {
      val prisonNumber = randomPrisonNumber()
      val requestBody = createRandomReligionInsertRequest()
      sendPostRequestAsserted<Unit>(
        url = prisonReligionCreateEndpoint(prisonNumber),
        body = requestBody,
        roles = listOf(PRISON_API_READ_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitAssert {
        assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)).isEmpty()
      }
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = prisonReligionCreateEndpoint(randomPrisonNumber()),
        body = createRandomReligionInsertRequest(),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = prisonReligionCreateEndpoint(randomPrisonNumber()),
        body = createRandomReligionInsertRequest(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun assertPrisonReligionEntityColumns(
    prisonNumber: String,
    actual: PrisonReligionEntity,
    expected: PrisonReligionInsertRequest,
  ) {
    assertThat(actual.updateId.toString()).isNotEmpty()
    assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
    assertThat(actual.code).isEqualTo(expected.religion)
    assertThat(actual.changeReasonKnown).isEqualTo(expected.comment?.isNotBlank() != null)
    assertThat(actual.comments).isEqualTo(expected.comment)
    assertThat(actual.startDate).isEqualTo(LocalDate.now())
    assertThat(actual.endDate).isNull()
    assertThat(actual.prisonRecordType).isEqualTo(PrisonRecordType.CURRENT)
    assertThat(actual.createUserId).isEqualTo(expected.userId)
    assertThat(actual.modifyUserId).isNull()
    assertThat(actual.modifyDateTime).isNull()
  }

  private fun createRandomReligionInsertRequest(excludedReligion: ReligionCode? = null) = PrisonReligionInsertRequest(
    religion = ReligionCode.entries.filterNot { it == excludedReligion }.random(),
    comment = "Some information",
    userId = "ABCDEF",
  )

  private fun prisonReligionCreateEndpoint(prisonNumber: String) = "/person/prison/$prisonNumber/religion"
}
