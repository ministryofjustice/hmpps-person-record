package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonWriteAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Creation {

    @Test
    fun `sending a current religion - saves religion record - updates current religion`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      val requestBody = createRandomReligion()
      webTestClient
        .post()
        .uri(prisonReligionPostEndpoint(prisonNumber))
        .bodyValue(requestBody)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isCreated

      awaitAssert {
        val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id = $prisonNumber")
        assertThat(personEntity.religion).isEqualTo(requestBody.religionCode)

        val prisonReligionEntities = prisonReligionRepository.findByPrisonNumber(prisonNumber)
        assertThat(prisonReligionEntities).hasSize(1)
        val actualPrisonReligion = prisonReligionEntities.first()
        assertThat(actualPrisonReligion.updateId.toString()).isNotEmpty()
        assertThat(actualPrisonReligion.prisonNumber).isEqualTo(prisonNumber)
        assertThat(actualPrisonReligion.changeReasonKnown).isEqualTo(requestBody.changeReasonKnown)
        assertThat(actualPrisonReligion.comments).isEqualTo(requestBody.comments)
        assertThat(actualPrisonReligion.verified).isEqualTo(requestBody.verified)
        assertThat(actualPrisonReligion.startDate).isEqualTo(requestBody.startDate)
        assertThat(actualPrisonReligion.endDate).isEqualTo(requestBody.endDate)
        assertThat(actualPrisonReligion.modifyDateTime).isEqualTo(requestBody.modifyDateTime)
        assertThat(actualPrisonReligion.prisonRecordType).isEqualTo(PrisonRecordType.from(requestBody.current))
      }
    }

    @Test
    fun `sending a non current religion - saves religion record - does not current religion`() {
    }

    @Test
    fun `person has existing religions records - saves religion record`() {
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `no religions sent - returns 400 bad request`() {
    }

    @Test
    fun `bad request body - returns 400 bad request`() {
    }

    @Test
    fun `person does not exist - returns 404 not found`() {
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
    }
  }

  private fun prisonReligionPostEndpoint(prisonNumber: String) = "/person/prison/$prisonNumber/religion"
}
