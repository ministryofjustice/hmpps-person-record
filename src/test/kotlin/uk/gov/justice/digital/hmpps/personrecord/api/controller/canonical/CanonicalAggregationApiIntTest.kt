package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName

class CanonicalAggregationApiIntTest : WebTestBase() {
  @Test
  fun `should return latest modified from 2 records  aggregate `() {
    val latestFirstName = randomName()
    val personKey = createPersonKey()
      .addPerson(
        Person.from(ProbationCase(name = ProbationCaseName(firstName = randomName(), middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      ).addPerson(
        Person.from(ProbationCase(name = ProbationCaseName(firstName = latestFirstName, middleNames = randomName(), lastName = randomName()), identifiers = Identifiers(crn = randomCrn()))),
      )

    val responseBody = webTestClient.get()
      .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CanonicalRecord::class.java)
      .returnResult()
      .responseBody!!

    assertThat(responseBody.firstName).isEqualTo(latestFirstName)
  }

  private fun canonicalAPIUrlAggregate(uuid: String) = "/canonical-record/$uuid"
}
