package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_SEARCH_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PersonSearchAPIControllerE2ETest : E2ETestBase() {

  @Test
  fun `should return persons that exists`() {
    val crn = randomCrn()
    val probationCase = createRandomProbationCase(crn)
    probationCreateEventAndResponseSetup(ApiResponseSetup.from(probationCase))
    val personEntity = awaitNotNull { personRepository.findByCrn(crn) }
    val person = Person.from(personEntity)

    val responseBody = webTestClient.post()
      .uri("/person/search")
      .authorised(listOf(API_SEARCH_ONLY))
      .bodyValue(
        PersonSearchRequest(
          firstName = person.firstName!!,
          lastName = person.lastName!!,
          middleName = person.middleNames!!,
          dateOfBirth = person.dateOfBirth!!,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<PersonSearchResponse>()
      .returnResult()
      .responseBody!!

    val actualSearchResult = responseBody.data.first()
    assertThat(actualSearchResult.name.firstName).isEqualTo(person.firstName)
    assertThat(actualSearchResult.name.lastName).isEqualTo(person.lastName)
    assertThat(actualSearchResult.name.dateOfBirth).isEqualTo(person.dateOfBirth)
  }
}
