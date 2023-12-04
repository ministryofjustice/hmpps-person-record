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
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
      defendantId = "39222c4f-97a6-401b-8669-2205ae629822",
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
  fun `should return HTTP Location header containing the URL of new person`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPerson)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
    )
      .andReturn()

    // Then
    // TODO
  }

  @Test
  fun `should persist and return a Person record with ID when minimum data set is provided`() {
    // Given
    val personJson = objectMapper.writeValueAsString(minimumPerson)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
    )
      .andExpect(status().isCreated)
      .andReturn()

    // Then
    val person = objectMapper.readValue(result.response.contentAsString, Person::class.java)
    assertThat(person.personId).isNotNull()

    person.personId?.let {
      val personEntity = personRepository.findByPersonId(it)
      assertNotNull(personEntity)
    }
  }

  @Test
  fun `should persist and return a Person record with ID and create an offender when a crn is provided`() {
    // Given
    maximumPerson = Person(
      otherIdentifiers = OtherIdentifiers(
        crn = "CRN404",
      ),
    )
    val personJson = objectMapper.writeValueAsString(maximumPerson)
    println(personJson)

    // When
    val result = mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
    )
      .andExpect(status().isCreated)
      .andReturn()

    // Then
    val person = objectMapper.readValue(result.response.contentAsString, Person::class.java)
    assertThat(person.personId).isNotNull()

    person.personId?.let {
      val personEntity = personRepository.findByPersonId(it)
      val offender = personEntity?.offenders?.get(0)
      if (offender != null) {
        assertEquals("CRN404", offender.crn)
      }
    }
  }

  @Test
  fun `should return HTTP Bad Response code when invalid UUID is provided to get person`() {
    // Given
    val badUuid = "BAD-UUID"

    // When
    mockMvc.perform(
      get("/person/$badUuid")
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
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
  fun `should return HTTP Conflict when an already existing CRN is provided to create person`() {
    // Given
    maximumPerson = Person(
      givenName = "Stephen",
      middleNames = listOf("Danny", "Alex"),
      familyName = "Jones",
      dateOfBirth = LocalDate.of(1968, 8, 15),
      otherIdentifiers = OtherIdentifiers(
        pncNumber = "PNC1234",
        crn = "CRN1234", // crn already exist in the db
      ),
    )
    val personJson = objectMapper.writeValueAsString(maximumPerson)

    mockMvc.perform(
      post("/person")
        .contentType(MediaType.APPLICATION_JSON)
        .content(personJson)
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
    )
      .andExpect(status().isConflict)
  }

  @Test
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
    )
      .andExpect(status().is2xxSuccessful)
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andReturn()

    // Then
    val person = objectMapper.readValue(result.response.contentAsString, Person::class.java)
    assertThat(person).isNotNull
    assertThat(person.personId).isEqualTo(UUID.fromString(uuid))
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
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
        .headers(setAuthorisation(roles = listOf(VIEW_PERSON_DATA_ROLE))),
    )
      .andExpect(status().isBadRequest)
  }
}
