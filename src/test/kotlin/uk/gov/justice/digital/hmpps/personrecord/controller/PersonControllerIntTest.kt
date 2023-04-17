package uk.gov.justice.digital.hmpps.personrecord.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDTO
import java.time.LocalDate

@Sql(
  scripts = ["classpath:sql/before-test.sql"],
  config = SqlConfig(transactionMode = TransactionMode.ISOLATED),
  executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
  scripts = ["classpath:sql/after-test.sql"],
  config = SqlConfig(transactionMode = TransactionMode.ISOLATED),
  executionPhase = ExecutionPhase.AFTER_TEST_METHOD,
)
class PersonControllerIntTest() : IntegrationTestBase() {

  @Test
  fun `should return HTTP Bad Response code when invalid UUID is provided to get person`() {
    // Given
    val badUuid = "BAD-UUID"

    // When
    mockMvc.perform(
      get("/person/$badUuid")
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should return HTTP Unauthorised when no role is provided`() {
    // Given
    val uuid = "eed4a9a4-d853-11ed-afa1-0242ac120002"

    // When
    mockMvc.perform(get("/person/$uuid"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `should return HTTP forbidden for an unauthorised role`() {
    // Given
    val uuid = "eed4a9a4-d853-11ed-afa1-0242ac120002"

    // When
    mockMvc.perform(
      get("/person/$uuid")
        .headers(setAuthorisation(roles = listOf("BAD_ROLE"))),
    )
      .andExpect(status().isForbidden)
  }

  @Test
  fun `should return HTTP not found when person id does not exist`() {
    // Given
    val unknownPersonId = "a455954c-d938-11ed-afa1-0242ac120002"

    // When
    mockMvc.perform(
      get("/person/$unknownPersonId")
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should return person record for known UUID`() {
    // Given
    val uuid = "eed4a9a4-d853-11ed-afa1-0242ac120002"

    // When
    val result = mockMvc.perform(
      get("/person/$uuid")
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().is2xxSuccessful)
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andReturn()

    // Then
    val personDto = objectMapper.readValue(result.response.contentAsString, PersonDTO::class.java)
    assertThat(personDto).isNotNull
    assertThat(personDto.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(personDto.otherIdentifiers?.pncNumber).isEqualTo("PNC12345")
    assertThat(personDto.givenName).isEqualTo("Carey")
    assertThat(personDto.familyName).isEqualTo("Mahoney")
    assertThat(personDto.dateOfBirth).isEqualTo(LocalDate.of(1965, 6, 18))
  }
}
