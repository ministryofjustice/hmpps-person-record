package uk.gov.justice.digital.hmpps.personrecord.controller

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.time.LocalDate
import java.util.UUID

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

  private var minimumPersonDetails: PersonDetails? = null
  private var maximumPersonDetails: PersonDetails? = null

  @Autowired
  lateinit var personRepository: PersonRepository

  @BeforeEach
  fun setUp() {
    minimumPersonDetails = PersonDetails(
      familyName = "Panchali",
      dateOfBirth = LocalDate.of(1968, 8, 15),
    )
    maximumPersonDetails = PersonDetails(
      givenName = "Stephen",
      middleNames = listOf("Danny", "Alex"),
      familyName = "Jones",
      dateOfBirth = LocalDate.of(1968, 8, 15),
      otherIdentifiers = OtherIdentifiers(
        pncNumber = "PNC1234",
        crn = "CRN8474",
      ),
    )
  }

  @Test
  fun `should return HTTP 401 when insufficient data is provide to create a Person record`() {
    // Given
    val personJson = "{}"

    // When
    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should return HTTP Location header containing the URL of new person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPersonDetails)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andReturn()

    // Then
    val personEntityList = personRepository.findByFamilyName("Panchali")
    assertThat(personEntityList).hasSize(1)

    val locationHeader = result.response.getHeader("Location")
    assertThat(locationHeader).contains("/person/${personEntityList[0].personId}")
  }

  @Test
  fun `should persist and return a Person record with ID when minimum data set is provided`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPersonDetails)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isCreated)
      .andReturn()

    // Then
    val personDetails = objectMapper.readValue(result.response.contentAsString, PersonDetails::class.java)
    assertThat(personDetails.personId).isNotNull()

    personDetails.personId?.let {
      val personEntity = personRepository.findByPersonId(it)
      assertThat(personEntity?.familyName).isEqualTo(minimumPersonDetails?.familyName)
      assertThat(personEntity?.dateOfBirth).isEqualTo(minimumPersonDetails?.dateOfBirth)
    }
  }

  @Test
  fun `should persist and return a Person record with ID when full data set is provided`() {
    // Given
    val personJson = objectMapper.writeValueAsString(maximumPersonDetails)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isCreated)
      .andReturn()

    // Then
    val personDetails = objectMapper.readValue(result.response.contentAsString, PersonDetails::class.java)
    assertThat(personDetails.personId).isNotNull()

    personDetails.personId?.let {
      val personEntity = personRepository.findByPersonId(it)
      assertThat(personEntity?.givenName).isEqualTo(maximumPersonDetails?.givenName)
      assertThat(personEntity?.familyName).isEqualTo(maximumPersonDetails?.familyName)
      assertThat(personEntity?.dateOfBirth).isEqualTo(maximumPersonDetails?.dateOfBirth)
      assertThat(personEntity?.middleNames).isEqualTo(maximumPersonDetails?.middleNames?.joinToString(" "))
      assertThat(personEntity?.crn).isEqualTo(maximumPersonDetails?.otherIdentifiers?.crn)
      assertThat(personEntity?.pncNumber).isEqualTo(maximumPersonDetails?.otherIdentifiers?.pncNumber)
    }
  }

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
  fun `should return HTTP Unauthorised when no role is provided to get person by id`() {
    // Given
    val uuid = "eed4a9a4-d853-11ed-afa1-0242ac120002"

    // When
    mockMvc.perform(get("/person/$uuid"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `should return HTTP forbidden for an unauthorised role to get person by id`() {
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
  fun `should return HTTP Unauthorised when no role is provided to create person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPersonDetails)

    // When
    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson),
    )
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `should return HTTP forbidden for an unauthorised role to to create person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPersonDetails)

    // When
    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
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
    val personDetails = objectMapper.readValue(result.response.contentAsString, PersonDetails::class.java)
    assertThat(personDetails).isNotNull
    assertThat(personDetails.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(personDetails.otherIdentifiers?.pncNumber).isEqualTo("PNC12345")
    assertThat(personDetails.givenName).isEqualTo("Carey")
    assertThat(personDetails.familyName).isEqualTo("Mahoney")
    assertThat(personDetails.dateOfBirth).isEqualTo(LocalDate.of(1965, 6, 18))
  }

  @Test
  fun `should return single search result for exact match`() {
    // Given
    val personSearchRequest = PersonSearchRequest(
      pncNumber = "PNC12345",
      crn = "CRN1234",
      forename = "Carey",
      surname = "Mahoney",
      dateOfBirth = LocalDate.of(1965, 6, 18),
    )
    val searchRequestJson = objectMapper.writeValueAsString(personSearchRequest)

    // When
    val result = mockMvc.perform(
      post("/person/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(searchRequestJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isOk)
      .andReturn()

    // Then
    val personList: List<PersonDetails> =
      objectMapper.readValue(result.response.contentAsString, object : TypeReference<List<PersonDetails>>() {})
    assertThat(personList).hasSize(1)
    assertThat(personList[0].personId).isEqualTo(UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"))
  }

  @Test
  fun `should return all matched person records for search request`() {
    // Given
    val personSearchRequest = PersonSearchRequest(surname = "EVANS")
    val searchRequestJson = objectMapper.writeValueAsString(personSearchRequest)

    // When
    val result = mockMvc.perform(
      post("/person/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(searchRequestJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isOk)
      .andReturn()

    // Then
    val personList: List<PersonDetails> =
      objectMapper.readValue(result.response.contentAsString, object : TypeReference<List<PersonDetails>>() {})
    assertThat(personList).hasSize(3).allMatch { it.familyName == "Evans" }
  }

  @Test
  fun `should return a single person record for a matching pnc number`() {
    // Given
    val personSearchRequest = PersonSearchRequest(pncNumber = "pnc33333", surname = "Evans")
    val searchRequestJson = objectMapper.writeValueAsString(personSearchRequest)

    // When
    val result = mockMvc.perform(
      post("/person/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(searchRequestJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isOk)
      .andReturn()

    // Then
    val personList: List<PersonDetails> =
      objectMapper.readValue(result.response.contentAsString, object : TypeReference<List<PersonDetails>>() {})
    assertThat(personList).hasSize(1).allMatch { it.otherIdentifiers?.pncNumber == "PNC33333" }
  }

  @Test
  fun `should return HTTP 401 when insufficient data is provide to search for a Person record`() {
    // Given
    val personSearchRequest = "{}"

    // When
    mockMvc.perform(
      post("/person/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personSearchRequest)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isBadRequest)
  }
}
