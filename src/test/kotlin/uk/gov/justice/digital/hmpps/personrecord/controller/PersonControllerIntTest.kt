package uk.gov.justice.digital.hmpps.personrecord.controller

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.time.LocalDate
import java.util.*

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

  private var minimumPerson: Person? = null
  private var maximumPerson: Person? = null

  @Autowired
  lateinit var personRepository: PersonRepository

  @BeforeEach
  fun setUp() {
    minimumPerson = Person(
      familyName = "Panchali",
    )
    maximumPerson = Person(
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
  @Disabled("Until refactoring complete")
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
    val personJson = objectMapper.writeValueAsString(minimumPerson)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andReturn()

    // Then
    // TODO
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should persist and return a Person record with ID when minimum data set is provided`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPerson)

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
    val person = objectMapper.readValue(result.response.contentAsString, Person::class.java)
    assertThat(person.personId).isNotNull()

    person.personId?.let {
      val personEntity = personRepository.findByPersonId(it)
    }
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should persist and return a Person record with ID when full data set is provided`() {
    // Given
    val personJson = objectMapper.writeValueAsString(maximumPerson)
    println(personJson)

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
    val person = objectMapper.readValue(result.response.contentAsString, Person::class.java)
    assertThat(person.personId).isNotNull()

    person.personId?.let {
      val personEntity = personRepository.findByPersonId(it)
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
  @Disabled("Until endpoint is secured by role")
  fun `should return HTTP Unauthorised when no role is provided to get person by id`() {
    // Given
    val uuid = "eed4a9a4-d853-11ed-afa1-0242ac120002"

    // When
    mockMvc.perform(get("/person/$uuid"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  @Disabled("Until endpoint is secured by role")
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
  @Disabled("Until endpoint is secured by role")
  fun `should return HTTP Unauthorised when no role is provided to create person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPerson)

    // When
    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson),
    )
      .andExpect(status().isUnauthorized)
  }

  @Test
  @Disabled("Until refactoring complete")
  fun `should return HTTP Conflict when an already existing CRN is provided to create person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(maximumPerson)

    // When
    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson),
    )
      .andExpect(status().isCreated)

    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson),
    )
      .andExpect(status().isConflict)
  }

  @Test
  @Disabled("Until endpoint is secured by role")
  fun `should return HTTP forbidden for an unauthorised role to to create person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPerson)

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
  @Disabled("Until refactoring complete")
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
    val person = objectMapper.readValue(result.response.contentAsString, Person::class.java)
    assertThat(person).isNotNull
    assertThat(person.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(person.otherIdentifiers?.pncNumber).isEqualTo("PNC12345")
    assertThat(person.givenName).isEqualTo("Carey")
    assertThat(person.familyName).isEqualTo("Mahoney")
    assertThat(person.dateOfBirth).isEqualTo(LocalDate.of(1965, 6, 18))
  }

  @Test
  fun `should return single search result for known CRN in search request`() {
    // Given
    val personSearchRequest = PersonSearchRequest(
      pncNumber = "PNC12345",
      crn = "CRN1234",
      forenameOne = "Carey",
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
    val personList: List<Person> = objectMapper.readValue(result.response.contentAsString, object : TypeReference<List<Person>>() {})
    assertThat(personList).hasSize(1)
    assertThat(personList[0].personId).isEqualTo(UUID.fromString("d75a9374-e2a3-11ed-b5ea-0242ac120002"))
  }

  @Test
  fun `should return HTTP not found for unknown CRN in search request`() {
    // Given
    val personSearchRequest = PersonSearchRequest(crn = "CRN3737")
    val searchRequestJson = objectMapper.writeValueAsString(personSearchRequest)

    // When
    mockMvc.perform(
      post("/person/search")
        .contentType(MediaType.APPLICATION_JSON)
        .content(searchRequestJson)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_PRISONER_DATA"))),
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should return all matched person records for search request`() {
    // Given
    val personSearchRequest = PersonSearchRequest(surname = "Mahoney")
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
    val personList: List<Person> =
      objectMapper.readValue(result.response.contentAsString, object : TypeReference<List<Person>>() {})
    assertThat(personList).hasSize(2)
      .extracting("personId")
      .contains(UUID.fromString("d75a9374-e2a3-11ed-b5ea-0242ac120002"), UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002"))
  }

  @Test
  fun `should return a single person record for a matching pnc number`() {
    // Given
    val personSearchRequest = PersonSearchRequest(pncNumber = "pnc33333", surname = "Mortimer")
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
    val personList: List<Person> =
      objectMapper.readValue(result.response.contentAsString, object : TypeReference<List<Person>>() {})
    assertThat(personList).hasSize(1).allMatch { it.personId == UUID.fromString("ddf11834-e2a3-11ed-b5ea-0242ac120002") }
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
