package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import java.util.UUID

class CanonicalApiServerErrorTest : WebTestBase() {

  @MockitoSpyBean
  override lateinit var personKeyRepository: PersonKeyRepository

  @Test
  fun `should return custom internal server error message when 500 error occurs`() {
    val cprUUID = UUID.randomUUID()
    doThrow(RuntimeException()).whenever(personKeyRepository).findByPersonUUID(cprUUID)
    webTestClient.get()
      .uri("/person/$cprUUID")
      .authorised(listOf(API_READ_ONLY))
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("userMessage").isEqualTo("Internal Server Error")
  }
}
