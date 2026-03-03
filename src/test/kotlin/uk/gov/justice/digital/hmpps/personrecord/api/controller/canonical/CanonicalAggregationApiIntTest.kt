package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class CanonicalAggregationApiIntTest : WebTestBase() {
  @Test
  fun `should return latest modified from 2 records combining aliases `() {
    val prisonDetails = createRandomPrisonPersonDetails()
    val probationDetails = createRandomProbationPersonDetails()

    val prisonPerson = createPerson(prisonDetails)
    val probationPerson = createPerson(probationDetails)

    val personKey = createPersonKey()
      .addPerson(prisonPerson)
      .addPerson(probationPerson)

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    val canonicalAlias = CanonicalAlias.from(prisonPerson)!! + CanonicalAlias.from(probationPerson)!!

    assertThat(responseBody.firstName).isEqualTo(probationDetails.firstName)
    assertThat(responseBody.aliases).isEqualTo(canonicalAlias)
  }

  @Test
  fun `should return latest modified from 2 records deduplicating aliases `() {
    val prisonDetails = createRandomPrisonPersonDetails()
    val probationDetails = createRandomProbationPersonDetails()

    val prisonPerson = createPerson(prisonDetails)
    val latestPerson = createPerson(probationDetails.copy(aliases = prisonDetails.aliases))

    val personKey = createPersonKey()
      .addPerson(prisonPerson)
      .addPerson(latestPerson)

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    val canonicalAlias = CanonicalAlias.from(latestPerson)!!

    assertThat(responseBody.firstName).isEqualTo(probationDetails.firstName)
    assertThat(responseBody.aliases).isEqualTo(canonicalAlias)
  }

  private fun canonicalAPIUrlAggregate(uuid: String) = "/canonical-record/$uuid"
}
