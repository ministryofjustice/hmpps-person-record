package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class PrisonAPIDeleteControllerIntTest : WebTestBase() {

  @Nested
  inner class SinglePersonCluster {

    @Test
    fun `deletes person data - deletes cluster - deletes from person match`() {
      val person = createRandomPrisonPersonDetails()
      val prisonNumber = person.prisonNumber!!

      val cluster = createPersonKey()
        .addPerson(person)
        .also { stubDeletePersonMatch() }

      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(prisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(prisonNumber)).isNull()
        assertThat(personKeyRepository.findByPersonUUID(cluster.personUUID)).isNull()
      }
    }
  }

  @Nested
  inner class MultiPersonCluster {

    @Test
    fun `deletes person data - does not delete cluster - deletes from person match`() {
      val personToBeDeleted = createRandomPrisonPersonDetails()
      val personToStayOnCluster = createRandomPrisonPersonDetails()
      val prisonNumber = personToBeDeleted.prisonNumber!!

      val clusterBeforeDelete = createPersonKey()
        .addPerson(personToBeDeleted)
        .addPerson(personToStayOnCluster)
        .also { stubDeletePersonMatch() }

      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(prisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(prisonNumber)).isNull()
        val clusterAfterDelete = personKeyRepository.findByPersonUUID(clusterBeforeDelete.personUUID)!!
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
        url = prisonPersonDeleteUrl("9999999999999"),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.NOT_FOUND,
      )

      awaitNotNull { personRepository.findByPrisonNumber(prisonNumber) }
      wiremock.verify(0, deleteRequestedFor(urlEqualTo("/person")))
    }
  }

  @Nested
  inner class Authorisation

  private fun prisonPersonDeleteUrl(prisonNumber: String) = "/person/prison/$prisonNumber"
}
