package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.religion

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionGetResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.test.generateUUIDString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonReligionGetApiControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class PrisonReligionExists {

    @Test
    fun `should return prison religion by cpr religion id`() {
      val prisonNumber = randomPrisonNumber()
      val prisonReligion = createRandomReligion()
      val existingReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligion))

      val responseBody = sendGetRequestAsserted<PrisonReligionGetResponseBody>(
        url = prisonReligionGetEndpoint(prisonNumber, existingReligionEntity.updateId.toString()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      val expectedResponseBody = PrisonReligionGetResponseBody.from(prisonNumber, existingReligionEntity)
      responseBody.isEqualTo(expectedResponseBody)
    }
  }

  @Nested
  inner class PrisonReligionDoesNotExists {

    @Test
    fun `should 404 not found`() {
      sendGetRequestAsserted<Unit>(
        url = prisonReligionGetEndpoint(randomPrisonNumber(), generateUUIDString()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendGetRequestAsserted<Unit>(
        url = prisonReligionGetEndpoint(randomPrisonNumber(), generateUUIDString()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendGetRequestAsserted<Unit>(
        url = prisonReligionGetEndpoint(randomPrisonNumber(), generateUUIDString()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonReligionGetEndpoint(prisonNumber: String, cprReligionId: String) = "/person/prison/$prisonNumber/religion/$cprReligionId"
}
