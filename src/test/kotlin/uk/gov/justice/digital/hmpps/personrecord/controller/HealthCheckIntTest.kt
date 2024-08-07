package uk.gov.justice.digital.hmpps.personrecord.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.health.HealthInfo
import uk.gov.justice.digital.hmpps.personrecord.health.PersonMatchHealthPing
import uk.gov.justice.digital.hmpps.personrecord.health.PersonRecordHealthPing
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckIntTest : WebTestBase() {

  @Autowired
  private lateinit var buildProperties: BuildProperties

  @MockBean
  @Autowired
  private lateinit var personMatchHealthPing: PersonMatchHealthPing

  @MockBean
  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

  @Autowired
  private lateinit var healthInfo: HealthInfo

  @BeforeEach
  fun setup() {
    `when`(personMatchHealthPing.health()).thenReturn(Health.up().build())
    `when`(personRecordHealthPing.health()).thenReturn(Health.up().build())

    healthInfo = HealthInfo(buildProperties)
  }

  @Test
  fun `Health page reports ok`() {
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
}
