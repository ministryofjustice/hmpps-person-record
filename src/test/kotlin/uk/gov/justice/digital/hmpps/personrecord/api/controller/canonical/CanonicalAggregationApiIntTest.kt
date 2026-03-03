package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class CanonicalAggregationApiIntTest : WebTestBase() {
  @Test
  fun `should return latest modified from 2 records  aggregate `() {
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


    val canonicalAlias = CanonicalAlias.from(prisonPerson)?.plus( CanonicalAlias.from(probationPerson))


    assertThat(responseBody.firstName).isEqualTo(probationDetails.firstName)
    assertThat(responseBody.aliases).isEqualTo(canonicalAlias?.get(0))


  }

  private fun canonicalAPIUrlAggregate(uuid: String) = "/canonical-record/$uuid"
}
