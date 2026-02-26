package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonWriteAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class PersonHasNoExistingPrisonReligionRecords {

    @Test
    fun `sending a current religion - saves religion - updates current religion`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion(current = true)
      sendRequestAsserted(prisonNumber, requestBody, HttpStatus.CREATED)

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(requestBody.religionCode)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
        assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligionEntities.first(), requestBody)
      }
    }

    @Test
    fun `sending a non current religion - saves religion - does not update current religion`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion(current = false)
      sendRequestAsserted(prisonNumber, requestBody, HttpStatus.CREATED)

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(null)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
        assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligionEntities.first(), requestBody)
      }
    }
  }

  @Nested
  inner class PersonHasExistingPrisonReligionRecords {

    @Nested
    inner class ExistingIsCurrent {
      @Test
      fun `sending a current religion - does not save religion - does not update current religion`() {
        val prisonNumber = randomPrisonNumber()
        val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
        val personEntityWithCurrentReligion = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingReligionEntity.code))
        prisonReligionRepository.save(existingReligionEntity)
        personRepository.saveAndFlush(personEntityWithCurrentReligion)

        val requestBody = createRandomReligion(current = true)
        sendRequestAsserted(prisonNumber, requestBody, HttpStatus.BAD_REQUEST)

        awaitAssert {
          val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
          assertThat(actualPersonEntity.religion).isEqualTo(existingReligionEntity.code)

          val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
          assertThat(actualPrisonReligionEntities).hasSize(1)
          assertThat(actualPrisonReligionEntities.first()).usingRecursiveComparison().isEqualTo(existingReligionEntity)
        }
      }

      @Test
      fun `sending a non current religion - saves religion - does not update current religion`() {
        val prisonNumber = randomPrisonNumber()
        val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
        val personEntityWithCurrentReligion = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingReligionEntity.code))
        prisonReligionRepository.save(existingReligionEntity)
        personRepository.saveAndFlush(personEntityWithCurrentReligion)

        val requestBody = createRandomReligion(current = false)
        sendRequestAsserted(prisonNumber, requestBody, HttpStatus.CREATED)

        awaitAssert {
          val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
          assertThat(actualPersonEntity.religion).isEqualTo(existingReligionEntity.code)

          val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
          assertThat(actualPrisonReligionEntities).hasSize(2)
          val actualCurrentReligionEntity = actualPrisonReligionEntities.first { it.prisonRecordType == PrisonRecordType.CURRENT }
          val actualNonCurrentReligionEntity = actualPrisonReligionEntities.first { it.prisonRecordType == PrisonRecordType.HISTORIC }
          assertThat(actualCurrentReligionEntity).usingRecursiveComparison().isEqualTo(existingReligionEntity)
          assertPrisonReligionEntityColumns(prisonNumber, actualNonCurrentReligionEntity, requestBody)
        }
      }
    }

    @Nested
    inner class ExistingIsNotCurrent {
      @Test
      fun `sending a current religion - saves religion - updates current religion`() {
        val prisonNumber = randomPrisonNumber()
        val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = false))
        val personEntityWithCurrentReligion = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = null))
        prisonReligionRepository.save(existingReligionEntity)
        personRepository.saveAndFlush(personEntityWithCurrentReligion)

        val requestBody = createRandomReligion(current = true)
        sendRequestAsserted(prisonNumber, requestBody, HttpStatus.CREATED)

        awaitAssert {
          val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
          assertThat(actualPersonEntity.religion).isEqualTo(requestBody.religionCode)

          val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
          assertThat(actualPrisonReligionEntities).hasSize(2)
          val actualNonCurrentReligionEntity = actualPrisonReligionEntities.first { it.prisonRecordType == PrisonRecordType.HISTORIC }
          val actualCurrentReligionEntity = actualPrisonReligionEntities.first { it.prisonRecordType == PrisonRecordType.CURRENT }
          assertThat(actualNonCurrentReligionEntity).usingRecursiveComparison().isEqualTo(existingReligionEntity)
          assertPrisonReligionEntityColumns(prisonNumber, actualCurrentReligionEntity, requestBody)
        }
      }
    }

    @Test
    fun `sending a non current religion - saves religion - does not update current religion`() {
      val prisonNumber = randomPrisonNumber()
      val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = false))
      val personEntityWithCurrentReligion = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = null))
      prisonReligionRepository.save(existingReligionEntity)
      personRepository.saveAndFlush(personEntityWithCurrentReligion)

      val requestBody = createRandomReligion(current = false)
      sendRequestAsserted(prisonNumber, requestBody, HttpStatus.CREATED)

      awaitAssert {
        val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(actualPersonEntity.religion).isEqualTo(null)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber).sortedBy { it.id }
        assertThat(actualPrisonReligionEntities).hasSize(2)
        assertThat(actualPrisonReligionEntities.first()).usingRecursiveComparison().isEqualTo(existingReligionEntity)
        assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligionEntities.last(), requestBody)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `bad request body - returns 400 bad request`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      // TODO: make return 400
      sendRequestAsserted(prisonNumber, null, HttpStatus.INTERNAL_SERVER_ERROR)

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(null)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(0)
      }
    }

    @Test
    fun `person does not exist - returns 404 not found`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion(current = true)
      sendRequestAsserted(randomPrisonNumber(), requestBody, HttpStatus.NOT_FOUND)

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
      webTestClient.post()
        .uri(prisonReligionPostEndpoint(randomPrisonNumber()))
        .bodyValue(createRandomReligion())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(prisonReligionPostEndpoint(randomPrisonNumber()))
        .bodyValue(createRandomReligion())
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }
  }

  private fun sendRequestAsserted(prisonNumber: String, prisonReligion: PrisonReligion?, expectedStatus: HttpStatus) {
    val res = webTestClient
      .post()
      .uri(prisonReligionPostEndpoint(prisonNumber))
      .bodyValue(prisonReligion ?: Any())
      .authorised(roles = listOf(PRISON_API_READ_WRITE))
      .exchange()

    when (expectedStatus) {
      HttpStatus.CREATED -> res.expectStatus().isCreated
      HttpStatus.BAD_REQUEST -> res.expectStatus().isBadRequest
      HttpStatus.NOT_FOUND -> res.expectStatus().isNotFound
      HttpStatus.INTERNAL_SERVER_ERROR -> res.expectStatus().is5xxServerError
      else -> fail("Unexpected status code $expectedStatus")
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
