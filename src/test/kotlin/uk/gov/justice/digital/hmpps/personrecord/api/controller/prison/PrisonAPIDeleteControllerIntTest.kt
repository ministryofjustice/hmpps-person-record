package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonAPIDeleteControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var mergeService: MergeService

  @Nested
  inner class SinglePersonCluster {

    @Test
    fun `deletes person - deletes cluster - deletes from person match`() {
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
    fun `deletes person - does not delete cluster - deletes from person match`() {
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
  inner class PersonMergeScenarios {

    @Test
    fun `deleting a merged from person - deletes from person - does not delete merged to person`() {
      val toPerson = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val fromPerson = createPerson(createRandomPrisonPersonDetails()) { mergedTo = toPerson.id }

      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(fromPerson.prisonNumber!!),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(fromPerson.prisonNumber!!)).isNull()
        wiremock.verify(0, deleteRequestedFor(urlEqualTo("/person")))
      }
    }

    @Test
    fun `deleting a merged to person - deletes to person - deletes all from descendants`() {
      val toPerson = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val fromPersonC = createPerson(createRandomPrisonPersonDetails()) { mergedTo = toPerson.id }
      val fromPersonB = createPerson(createRandomPrisonPersonDetails()) { mergedTo = toPerson.id }
      val fromPersonA = createPerson(createRandomPrisonPersonDetails()) { mergedTo = fromPersonB.id }

      stubDeletePersonMatch()

      sendDeleteRequestAsserted<Unit>(
        url = prisonPersonDeleteUrl(toPerson.prisonNumber!!),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = HttpStatus.OK,
      )

      awaitAssert {
        assertThat(personRepository.findByPrisonNumber(fromPersonA.prisonNumber!!)).isNull()
        assertThat(personRepository.findByPrisonNumber(fromPersonB.prisonNumber!!)).isNull()
        assertThat(personRepository.findByPrisonNumber(fromPersonC.prisonNumber!!)).isNull()
        assertThat(personRepository.findByPrisonNumber(toPerson.prisonNumber!!)).isNull()
        wiremock.verify(
          1,
          deleteRequestedFor(urlEqualTo("/person"))
            .withRequestBody(equalToJson("""{"matchId":"${toPerson.matchId}"}""")),
        )
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
