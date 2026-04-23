package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonAPIDeleteControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `deletes person by prison number`() {
      val personToBeDeleted = createPerson(createRandomPrisonPersonDetails())
      val cluster = createPersonKey()
        .addPerson(personToBeDeleted)
        .addPerson(createPerson(createRandomPrisonPersonDetails()))
        .also {
          stubDeletePersonMatch()
          stubPersonMatchScores()
        }

      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(personToBeDeleted.prisonNumber!!),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(personToBeDeleted.prisonNumber!!)).isNull()
        val clusterAfterDelete = personKeyRepository.findByPersonUUID(cluster.personUUID)!!
        assertThat(clusterAfterDelete.personEntities.size).isEqualTo(1)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `person does not exist - does not delete anything`() {
      val person = createRandomPrisonPersonDetails()
      val prisonNumber = person.prisonNumber!!

      createPersonKey()
        .addPerson(person)

      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(randomPrisonNumber()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      wiremock.verify(0, deleteRequestedFor(urlEqualTo("/person")))
    }
  }

  @Nested
  inner class Authorisation {
    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(randomPrisonNumber()),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonPersonDeleteUrl(prisonNumber: String) = "/person/prison/$prisonNumber"
}
