package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionGetResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonReligionGetApiControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class PrisonReligionExists {

    @Test
    fun `should return all prison religions by prison number`() {
      val prisonNumber = randomPrisonNumber()
      val prisonReligionOne = createRandomReligion()
      val prisonReligionTwo = createRandomReligion().copy(current = false)
      val existingReligionEntityOne = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligionOne))
      val existingReligionEntityTwo = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, prisonReligionTwo))

      val responseBody = sendGetRequestAsserted<PrisonReligionGetResponse>(
        url = prisonReligionGetEndpoint(prisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      val expectedResponseBody = PrisonReligionGetResponse.from(prisonNumber, listOf(existingReligionEntityOne, existingReligionEntityTwo))
      responseBody.isEqualTo(expectedResponseBody)
    }
  }

  @Nested
  inner class PrisonReligionDoesNotExists {

    @Test
    fun `should return empty list of prison religions for prison number`() {
      val prisonNumber = randomPrisonNumber()
      val responseBody = sendGetRequestAsserted<PrisonReligionGetResponse>(
        url = prisonReligionGetEndpoint(prisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      val expectedResponseBody = PrisonReligionGetResponse.from(prisonNumber, emptyList())
      responseBody.isEqualTo(expectedResponseBody)
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendGetRequestAsserted<Unit>(
        url = prisonReligionGetEndpoint(randomPrisonNumber()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendGetRequestAsserted<Unit>(
        url = prisonReligionGetEndpoint(randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonReligionGetEndpoint(prisonNumber: String) = "/person/prison/$prisonNumber/religion"
}
