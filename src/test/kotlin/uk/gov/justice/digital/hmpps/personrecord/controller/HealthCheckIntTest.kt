package uk.gov.justice.digital.hmpps.personrecord.controller
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckIntTest : WebTestBase() {

  @Test
  fun `Health page reports ok`() {
    stubGetRequest(url = "/health", body = """{"status": "UP"}""")

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health info reports version`() {
    stubGetRequest(url = "/health", body = """{"status": "UP"}""")
    webTestClient.get().uri("/health")
      .authorised()
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        },
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `should return OK for info endpoint`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
  }

  @Test
  fun `when person match is down, person record should be down`() {
    stubGetRequest(url = "/health", body = """{"status": "DOWN"}""", status = 500)

    webTestClient.get()
      .uri("/health")
      .authorised()
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("components.healthInfo.status").isEqualTo("DOWN")
  }

  @Test
  fun `verify match details are returned`() {
    stubGetRequest(url = "/health", body = """{"status": "UP"}""")
    webTestClient.get()
      .uri("/health")
      .authorised()
      .exchange()
      .expectStatus()
      .isOk
      .expectBody().jsonPath("components.healthInfo.details.PersonMatchStatus.status").isEqualTo("UP")
  }
}
