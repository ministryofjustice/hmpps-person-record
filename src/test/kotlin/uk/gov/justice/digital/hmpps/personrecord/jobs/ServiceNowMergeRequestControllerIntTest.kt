package uk.gov.justice.digital.hmpps.personrecord.jobs

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private const val GENERATE_MERGE_REQUESTS = "/jobs/service-now/generate-delius-merge-requests"

class ServiceNowMergeRequestControllerIntTest : WebTestBase() {

  @Value($$"${service-now.sysparm-id}")
  lateinit var sysParmId: String

  var serviceNowStub: StubMapping? = null

  @BeforeEach
  fun beforeEach() {
    serviceNowStub = wiremock.stubFor(
      WireMock.post(
        "/api/sn_sc/servicecatalog/items/$sysParmId/order_now",
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """{
        "result": {
          "sys_id": "20d3ba6b47a272106322862c736d437c",
          "number": "REQ2039412",
          "request_number": "REQ2039412",
          "parent_id": null,
          "request_id": "20d3ba6b47a272106322862c736d437c",
          "parent_table": "task",
          "table": "sc_request"
        }
      } 
              """.trimIndent(),
            ),
        ),
    )

    serviceNowAuthSetup()
  }

  fun serviceNowAuthSetup() {
    wiremock.stubFor(
      WireMock.post("/oauth_token.do")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
                {
                  "token_type": "bearer",
                  "access_token": "SOME_TOKEN",
                  "expires_in": ${LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).toEpochSecond(ZoneOffset.UTC)}
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  @Test
  fun `should pick five clusters where each cluster has more than one probation record`() {
    val crn1 = randomCrn()
    val crn2 = randomCrn()
    val crn3 = randomCrn()
    val crn4 = randomCrn()
    val crn5 = randomCrn()
    val crn6 = randomCrn()
    val crn7 = randomCrn()
    val crn8 = randomCrn()
    val crn9 = randomCrn()
    val crn10 = randomCrn()
    val crn11 = randomCrn()
    val crn12 = randomCrn()

    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn1))
      .addPerson(createRandomProbationPersonDetails(crn2))
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn3))
      .addPerson(createRandomProbationPersonDetails(crn4))
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn5))
      .addPerson(createRandomProbationPersonDetails(crn6))
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn7))
      .addPerson(createRandomProbationPersonDetails(crn8))
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn9))
      .addPerson(createRandomProbationPersonDetails(crn10))
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn11))
      .addPerson(createRandomProbationPersonDetails(crn12))

    personRepository.updateLastModifiedDate(crn1, LocalDateTime.now().minusDays(1).plusMinutes(1))
    personRepository.updateLastModifiedDate(crn2, LocalDateTime.now().minusDays(1).plusMinutes(2))
    personRepository.updateLastModifiedDate(crn3, LocalDateTime.now().minusDays(1).plusMinutes(3))
    personRepository.updateLastModifiedDate(crn4, LocalDateTime.now().minusDays(1).plusMinutes(4))
    personRepository.updateLastModifiedDate(crn5, LocalDateTime.now().minusDays(1).plusMinutes(5))
    personRepository.updateLastModifiedDate(crn6, LocalDateTime.now().minusDays(1).plusMinutes(6))
    personRepository.updateLastModifiedDate(crn7, LocalDateTime.now().minusDays(1).plusMinutes(7))
    personRepository.updateLastModifiedDate(crn8, LocalDateTime.now().minusDays(1).plusMinutes(8))
    personRepository.updateLastModifiedDate(crn9, LocalDateTime.now().minusDays(1).plusMinutes(9))
    personRepository.updateLastModifiedDate(crn10, LocalDateTime.now().minusDays(1).plusMinutes(10))
    personRepository.updateLastModifiedDate(crn11, LocalDateTime.now().minusDays(1).plusMinutes(11))
    personRepository.updateLastModifiedDate(crn12, LocalDateTime.now().minusDays(1).plusMinutes(12))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk
    wiremock.verify(5, RequestPatternBuilder.like(serviceNowStub?.request))
  }

  @Test
  fun `should not send a merge request for a cluster which has already had a merge request`() {
    val crn1 = randomCrn()
    val crn2 = randomCrn()

    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(crn1))
      .addPerson(createRandomProbationPersonDetails(crn2))

    personRepository.updateLastModifiedDate(crn1, LocalDateTime.now().minusDays(1).plusMinutes(1))
    personRepository.updateLastModifiedDate(crn2, LocalDateTime.now().minusDays(1).plusMinutes(2))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    wiremock.verify(1, RequestPatternBuilder.like(serviceNowStub?.request))
  }

  @Test
  fun `should not send a merge request for a cluster which has two prison records but no probation records`() {
    personRepository.deleteAll()
    val prisonNumber1 = randomPrisonNumber()
    val prisonNumber2 = randomPrisonNumber()

    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumber1))
      .addPerson(createRandomPrisonPersonDetails(prisonNumber2))

    personRepository.updatePrisonerLastModifiedDate(prisonNumber1, LocalDateTime.now().minusDays(1).plusMinutes(1))
    personRepository.updatePrisonerLastModifiedDate(prisonNumber2, LocalDateTime.now().minusDays(1).plusMinutes(2))

    webTestClient.post()
      .uri(GENERATE_MERGE_REQUESTS)
      .exchange()
      .expectStatus()
      .isOk

    wiremock.verify(0, RequestPatternBuilder.like(serviceNowStub?.request))
    wiremock.removeStub(serviceNowStub?.uuid)
  }

  private fun PersonRepository.updateLastModifiedDate(crn: String, lastModified: LocalDateTime) {
    val personEntity = findByCrn(crn)!!
    personEntity.lastModified = lastModified
    saveAndFlush(personEntity)
  }
  private fun PersonRepository.updatePrisonerLastModifiedDate(prisonNumber: String, lastModified: LocalDateTime) {
    val personEntity = findByPrisonNumber(prisonNumber)!!
    personEntity.lastModified = lastModified
    saveAndFlush(personEntity)
  }
}
