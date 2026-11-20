package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class SysconSyncPrisonPrimaryNameAPIControllerIntTest(
  @Autowired private val pseudonymRepository: PseudonymRepository,
) : WebTestBase() {

  @Nested
  inner class ProcessPrimaryName {

    @Nested
    inner class SuccessfulProcessing {

      @Test
      fun `should make the alias the primary name and the old primary name an alias`() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val oldPrimaryName = personEntity.getPrimaryName()
        val alias = personEntity.getAliases().single()

        webTestClient.post()
          .uri(primaryNameUrl(prisonNumber, alias.updateId.toString()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isNoContent

        val updatedPerson = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(updatedPerson.pseudonyms.filter { it.nameType == PRIMARY }.map { it.updateId }).containsExactly(alias.updateId)
        assertThat(updatedPerson.getAliases().map { it.updateId }).containsExactly(oldPrimaryName.updateId)
        assertThat(updatedPerson.getPrimaryName().lastName).isEqualTo(alias.lastName)
        wiremock.verify(1, postRequestedFor(urlEqualTo("/person")))
      }

      @Test
      fun `should do nothing when the alias is already the primary name`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val primaryName = personEntity.getPrimaryName()
        val alias = personEntity.getAliases().single()

        webTestClient.post()
          .uri(primaryNameUrl(prisonNumber, primaryName.updateId.toString()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isNoContent

        val unchangedPrimaryName = pseudonymRepository.findByUpdateId(primaryName.updateId!!)!!
        assertThat(unchangedPrimaryName.nameType).isEqualTo(PRIMARY)
        assertThat(unchangedPrimaryName.version).isEqualTo(primaryName.version)
        assertThat(pseudonymRepository.findByUpdateId(alias.updateId!!)!!.nameType).isEqualTo(ALIAS)
        wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `should return not found when the alias does not exist`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val cprAliasId = randomCprAliasId()

        webTestClient.post()
          .uri(primaryNameUrl(prisonNumber, cprAliasId))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("userMessage")
          .isEqualTo("Not found: Alias with $cprAliasId not found for person with $prisonNumber")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.post()
          .uri(primaryNameUrl(randomPrisonNumber(), randomCprAliasId()))
          .authorised(listOf("UNSUPPORTED-ROLE"))
          .exchange()
          .expectStatus()
          .isForbidden
          .expectBody()
          .jsonPath("userMessage")
          .isEqualTo("Forbidden: Access Denied")
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        webTestClient.post()
          .uri(primaryNameUrl(randomPrisonNumber(), randomCprAliasId()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  private fun randomCprAliasId() = UUID.randomUUID().toString()

  private fun primaryNameUrl(prisonNumber: String, cprAliasId: String) = "/syscon-sync/person/$prisonNumber/primary-name/$cprAliasId"
}
