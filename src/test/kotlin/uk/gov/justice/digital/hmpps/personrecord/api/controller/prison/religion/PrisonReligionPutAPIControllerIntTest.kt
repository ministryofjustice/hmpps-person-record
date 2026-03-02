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
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class PrisonReligionPutAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Successful {

    @Test
    fun `prison religion exists - updates prison religion - returns correct response body`() {
      val prisonNumber = randomPrisonNumber()
      val existingReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion(code = ReligionCode.AGNO.toString())))
      val existingPersonEntity = personRepository.saveAndFlush(PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingReligionEntity.code)))

      val requestBody = createRandomReligionUpdateRequest()
      val responseBody = sendPutRequestAsserted<PrisonReligionResponseBody>(
        url = prisonReligionPutEndpoint(prisonNumber, existingReligionEntity.updateId.toString()),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      ).also { prisonReligionRepository.flush() }

      awaitAssert {
        val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(actualPersonEntity.religion).isEqualTo(existingPersonEntity.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
        val actualPrisonReligion = actualPrisonReligionEntities.first()

        assertThat(actualPrisonReligion.updateId).isEqualTo(existingReligionEntity.updateId)
        assertThat(actualPrisonReligion.comments).isEqualTo(requestBody.comments)
        assertThat(actualPrisonReligion.verified).isEqualTo(requestBody.verified)
        assertThat(actualPrisonReligion.modifyDateTime).isEqualTo(requestBody.modifyDateTime)
        assertThat(actualPrisonReligion.modifyUserId).isEqualTo(requestBody.modifyUserId)
        assertThat(actualPrisonReligion.endDate).isEqualTo(requestBody.endDate)
        assertThat(actualPrisonReligion.prisonRecordType).isEqualTo(PrisonRecordType.from(requestBody.current))

        val expectedResponseBody = PrisonReligionResponseBody(prisonNumber, PrisonReligionMapping(requestBody.nomisReligionId, existingReligionEntity.updateId.toString()))
        responseBody.isEqualTo(expectedResponseBody)
      }
    }

    @Test
    fun `prison religion exists - demoting a current religion - update prison religion`() {
      val prisonNumber = randomPrisonNumber()
      val existingCurrentReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion(code = ReligionCode.AGNO.toString())))
      val existingPersonEntity = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingCurrentReligionEntity.code))
      personRepository.saveAndFlush(existingPersonEntity)

      val requestBody = createRandomReligionUpdateRequest(current = false)
      sendPutRequestAsserted<PrisonReligionResponseBody>(
        url = prisonReligionPutEndpoint(prisonNumber, existingCurrentReligionEntity.updateId.toString()),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(actualPersonEntity.religion).isEqualTo(existingPersonEntity.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities.first().prisonRecordType).isEqualTo(PrisonRecordType.HISTORIC)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `prison religion exists - promoting a non current religion - returns 400 bad request`() {
      val prisonNumber = randomPrisonNumber()
      val existingCurrentReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion(code = ReligionCode.AGNO.toString())))
      val existingNonCurrentReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = false, code = ReligionCode.BAHA.toString())))
      val existingPersonEntity = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingCurrentReligionEntity.code))
      personRepository.saveAndFlush(existingPersonEntity)

      val requestBody = createRandomReligionUpdateRequest(current = true)
      sendPutRequestAsserted<Unit>(
        url = prisonReligionPutEndpoint(prisonNumber, existingNonCurrentReligionEntity.updateId.toString()),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.BAD_REQUEST,
      )

      awaitAssert {
        val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(actualPersonEntity.religion).isEqualTo(existingPersonEntity.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber).associateBy { it.id }
        assertThat(actualPrisonReligionEntities.keys).hasSize(2)
        assertThat(actualPrisonReligionEntities[existingCurrentReligionEntity.id]).usingRecursiveComparison().isEqualTo(existingCurrentReligionEntity)
        assertThat(actualPrisonReligionEntities[existingNonCurrentReligionEntity.id]).usingRecursiveComparison().isEqualTo(existingNonCurrentReligionEntity)
      }
    }

    @Test
    fun `prison religion does not exist - returns 404 not found`() {
      val prisonNumber = randomPrisonNumber()
      val requestBody = createRandomReligionUpdateRequest()
      sendPutRequestAsserted<Unit>(
        url = prisonReligionPutEndpoint(prisonNumber, UUID.randomUUID().toString()),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitAssert {
        assertThat(prisonReligionRepository.findByPrisonNumber(prisonNumber)).isEmpty()
      }
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPutRequestAsserted<Unit>(
        url = prisonReligionPutEndpoint(randomPrisonNumber(), UUID.randomUUID().toString()),
        body = createRandomReligionUpdateRequest(),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPutRequestAsserted<Unit>(
        url = prisonReligionPutEndpoint(randomPrisonNumber(), UUID.randomUUID().toString()),
        body = createRandomReligionUpdateRequest(),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun createRandomReligionUpdateRequest(current: Boolean = true) = PrisonReligionUpdateRequest(
    nomisReligionId = randomPrisonNumber(),
    comments = randomName(),
    verified = randomBoolean(),
    endDate = randomDate(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    current = current,
  )

  private fun prisonReligionPutEndpoint(prisonNumber: String, cprReligionId: String) = "/person/prison/$prisonNumber/religion/$cprReligionId"
}
