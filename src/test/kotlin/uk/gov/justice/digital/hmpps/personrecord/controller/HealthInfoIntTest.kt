package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase

class HealthInfoIntTest() : IntegrationTestBase() {

  @Test
  fun `should return OK for health readiness endpoint`() {
    mockMvc.perform(
      get("/health/readiness"),
    ).andExpect(status().isOk)
  }

  @Test
  fun `should return OK for health liveness endpoint`() {
    mockMvc.perform(
      get("/health/liveness"),
    ).andExpect(status().isOk)
  }

  @Test
  fun `should return OK for info endpoint`() {
    mockMvc.perform(
      get("/info"),
    ).andExpect(status().isOk)
  }


}
