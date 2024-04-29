package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.util.concurrent.TimeUnit.SECONDS

class PopulateFromNomisIntTest : IntegrationTestBase() {

  @Test
  fun `populate from nomis`() {
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    // will have to change to personrepository but this will be near enough

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(defendantRepository.findAll().size).isEqualTo(7)
    }
    val prisoners = defendantRepository.findAll()
    assertThat(prisoners[0].firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoners[1].firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(prisoners[2].firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(prisoners[3].firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(prisoners[4].firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(prisoners[5].firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(prisoners[6].firstName).isEqualTo("PrisonerSevenFirstName")
  }

  @Test
  fun `populate from nomis retries getPrisonerNumbers`() {
    // first call fails
    wireMockExtension.stubFor(
      WireMock.get("/api/prisoners/getPrisonerNumbers?size=1&page=0").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(503),
      ),
    )
    // second call succeeds
    wireMockExtension.stubFor(
      WireMock.get("/api/prisoners/getPrisonerNumbers?size=1&page=0").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("{\n  \"totalPages\": 7,\n  \"totalElements\": 7,\n  \"first\": true,\n  \"last\": false,\n  \"size\": 0,\n  \"content\": [\n    \"prisonerNumberOne\"\n  ],\n  \"number\": 0,\n  \"sort\": [\n    {\n      \"direction\": \"string\",\n      \"nullHandling\": \"string\",\n      \"ascending\": true,\n      \"property\": \"string\",\n      \"ignoreCase\": true\n    }\n  ],\n  \"numberOfElements\": 0,\n  \"pageable\": {\n    \"offset\": 0,\n    \"sort\": [\n      {\n        \"direction\": \"string\",\n        \"nullHandling\": \"string\",\n        \"ascending\": true,\n        \"property\": \"string\",\n        \"ignoreCase\": true\n      }\n    ],\n    \"pageSize\": 0,\n    \"pageNumber\": 0,\n    \"unpaged\": true,\n    \"paged\": true\n  },\n  \"empty\": true\n}")
          .withStatus(200),

      ),
    )
    webTestClient.post()
      .uri("/populatefromnomis")
      .exchange()
      .expectStatus()
      .isOk

    // will have to change to personrepository but this will be near enough

    await.atMost(15, SECONDS) untilAsserted {
      assertThat(defendantRepository.findAll().size).isEqualTo(7)
    }
    val prisoners = defendantRepository.findAll()
    assertThat(prisoners[0].firstName).isEqualTo("PrisonerOneFirstName")
    assertThat(prisoners[1].firstName).isEqualTo("PrisonerTwoFirstName")
    assertThat(prisoners[2].firstName).isEqualTo("PrisonerThreeFirstName")
    assertThat(prisoners[3].firstName).isEqualTo("PrisonerFourFirstName")
    assertThat(prisoners[4].firstName).isEqualTo("PrisonerFiveFirstName")
    assertThat(prisoners[5].firstName).isEqualTo("PrisonerSixFirstName")
    assertThat(prisoners[6].firstName).isEqualTo("PrisonerSevenFirstName")
  }
}
