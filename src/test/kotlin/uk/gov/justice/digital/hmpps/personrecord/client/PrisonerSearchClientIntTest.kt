package uk.gov.justice.digital.hmpps.personrecord.client

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PossibleMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.time.LocalDate

class PrisonerSearchClientIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var restClient: PrisonerSearchClient

//  @BeforeEach
//  fun setup() {
//    wireMockExtension
//    wireMockServer.start()
//    configureFor("localhost", 8090)
//  }
//
//  @AfterEach
//  fun tearDown() {
//    wireMockServer.stop()
//  }

  @Test
  fun `should return prisoner details for given match criteria`() {
    // Given
    val dob = LocalDate.of(1960, 1, 1)
    val possibleMatchCriteria = PossibleMatchCriteria(
      firstName = "Eric",
      lastName = "Lassard",
      pncNumber = "2003/0062845E",
      dateOfBirth = dob,
    )

    // When
    val prisoners = restClient.findPossibleMatches(possibleMatchCriteria)

    // Then
    assertThat(prisoners).isNotEmpty.hasSize(1)
    assertThat(prisoners?.get(0)?.prisonerNumber).isEqualTo("A1234AA")
    assertThat(prisoners?.get(0)?.pncNumber).isEqualTo("2003/0062845E")
    assertThat(prisoners?.get(0)?.gender).isEqualTo("Male")
    assertThat(prisoners?.get(0)?.nationality).isEqualTo("Egyptian")
  }

  @Test
  fun `should return empty prisoner list for unmatched search criteria`() {
    // Given
    val possibleMatchCriteria = PossibleMatchCriteria(
      firstName = "Melanie",
      lastName = "Sykes",
    )

    wireMockExtension.givenThat(
      post(urlPathEqualTo("/prisoner-search/possible-matches"))
        .withRequestBody(equalTo(objectMapper.writeValueAsString(possibleMatchCriteria)))
        .withHeader("Content-Type", equalTo("application/json"))
        .willReturn(
          aResponse()
            .withBody("[]")
            .withStatus(200)
            .withHeader("Content-Type", "application/json"),
        ),
    )

    // When
    val prisoners = restClient.findPossibleMatches(possibleMatchCriteria)

    // Then
    assertThat(prisoners).isEmpty()
  }
}
