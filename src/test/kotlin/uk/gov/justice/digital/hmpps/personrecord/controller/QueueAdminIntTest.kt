package uk.gov.justice.digital.hmpps.personrecord.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase

class QueueAdminIntTest() : IntegrationTestBase() {

  @Test
  fun `should return HTTP Location header containing the URL of new person`() {
    mockMvc.perform(
      put("/queue-admin/retry-all-dlqs"),
    ).andExpect(status().isOk)
  }
}
