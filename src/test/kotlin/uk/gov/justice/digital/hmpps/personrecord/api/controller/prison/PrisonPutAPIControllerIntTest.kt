package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonPutAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Successful {

    @Test
    fun `prison religion exists - updates prison religion - returns correct response body`() {
      val prisonNumber = randomPrisonNumber()
      val existingReligionEntity = PrisonReligionEntity.from(prisonNumber, createRandomReligion(code = ReligionCode.AGNO.toString()))
      val personEntity = PersonEntity.new(createRandomPrisonPersonDetails(prisonNumber).copy(religion = existingReligionEntity.code))
      prisonReligionRepository.save(existingReligionEntity)
      personRepository.saveAndFlush(personEntity)

      val requestBody = createRandomReligion(code = ReligionCode.BAHA.toString())
      sendPutRequestAsserted<PrisonReligionResponseBody>(
        url = prisonReligionPutEndpoint(prisonNumber, existingReligionEntity.updateId.toString()),
        body = requestBody,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(personEntity.religion)

        val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(actualPrisonReligionEntities).hasSize(1)
        val actualPrisonReligion = actualPrisonReligionEntities.first()

        assertThat(actualPrisonReligion.comments).isEqualTo(requestBody.comments)
        assertThat(actualPrisonReligion.verified).isEqualTo(requestBody.verified)
        assertThat(actualPrisonReligion.modifyDateTime).isEqualTo(requestBody.modifyDateTime)
        assertThat(actualPrisonReligion.modifyUserId).isEqualTo(requestBody.modifyUserId)

        assertThat(actualPrisonReligion.updateId.toString()).isEqualTo(existingReligionEntity.updateId.toString())
        assertThat(actualPrisonReligion.prisonNumber).isEqualTo(existingReligionEntity.prisonNumber)
        assertThat(actualPrisonReligion.changeReasonKnown).isEqualTo(existingReligionEntity.changeReasonKnown)
        assertThat(actualPrisonReligion.startDate).isEqualTo(existingReligionEntity.startDate)
        assertThat(actualPrisonReligion.endDate).isEqualTo(existingReligionEntity.endDate)
        assertThat(actualPrisonReligion.prisonRecordType).isEqualTo(existingReligionEntity.prisonRecordType)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `person does not exist - returns 404 not found`() {
    }

    @Test
    fun `prison religion does not exist - returns 404 not found`() {
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
    }
  }

  private fun prisonReligionPutEndpoint(prisonNumber: String, cprReligionId: String) = "/person/prison/$prisonNumber/religion/$cprReligionId"
}
