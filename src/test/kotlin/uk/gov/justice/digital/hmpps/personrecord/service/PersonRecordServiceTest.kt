package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class PersonRecordServiceTest {

  @Mock
  lateinit var personRepository: PersonRepository

  @InjectMocks
  lateinit var personRecordService: PersonRecordService

  private lateinit var personEntity: PersonEntity
  private lateinit var dateOfBirth: LocalDate

  @BeforeEach
  fun setUp() {
    dateOfBirth = LocalDate.of(1969, 8, 20)
    personEntity = PersonEntity(
      id = 23232L,
      personId = UUID.randomUUID(),
      givenName = "Stephen",
      middleNames = "Michael James",
      dateOfBirth = dateOfBirth,
      familyName = "Jones",
      crn = "CRN1234",
      pncNumber = "PNC82882",
    )
  }

  @Test
  fun `should return person dto for known person id`() {
    // Given
    val personId = UUID.fromString("f4165b62-d9eb-11ed-afa1-0242ac120002")

    whenever(personRepository.findByPersonId(any())).thenReturn(personEntity)

    // When
    val personDTO = personRecordService.getPersonById(personId)

    // Then
    assertThat(personDTO.familyName).isEqualTo("Jones")
    assertThat(personDTO.givenName).isEqualTo("Stephen")
    assertThat(personDTO.middleNames).contains("Michael", "James")
    assertThat(personDTO.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(personDTO.otherIdentifiers?.crn).isEqualTo("CRN1234")
    assertThat(personDTO.otherIdentifiers?.pncNumber).isEqualTo("PNC82882")
  }

  @Test
  fun `should throw exception when person does not exist for supplied id`() {
    // Given
    whenever(personRepository.findByPersonId(any())).thenThrow(EntityNotFoundException("Person record not found for id"))

    // When
    val exception = assertFailsWith<EntityNotFoundException>(
      block = { personRecordService.getPersonById(UUID.randomUUID()) },
    )

    // Then
    assertThat(exception.message).isEqualTo("Person record not found for id")
  }

  @Test
  fun `should create a person record with unique person Identifier from supplied person dto`() {
    // Given
    val personDetails = PersonDetails(
      givenName = "Stephen",
      middleNames = listOf("Michael", "James"),
      familyName = "Jones",
      dateOfBirth = LocalDate.of(1968, 8, 15),
    )

    whenever(personRepository.save(any())).thenReturn(personEntity)

    // When
    val personRecord = personRecordService.createPersonRecord(personDetails)

    // Then
    verify(personRepository).save(any<PersonEntity>())
    assertThat(personRecord.personId).isNotNull
  }

  @Test
  fun `should return an empty list when search parameters do not match any person records`() {
    // Given
    val searchRequest = PersonSearchRequest(surname = "Unknown")
    whenever(personRepository.searchByRequestParameters(searchRequest)).thenReturn(emptyList())

    // When
    val personDetailsList = personRecordService.searchPersonRecords(searchRequest)

    // Then
    assertThat(personDetailsList).isEmpty()
  }

  @Test
  fun `should return all matching person records for provided search parameters`() {
    // Given
    val searchRequest = PersonSearchRequest(surname = "Jones")
    whenever(personRepository.searchByRequestParameters(searchRequest))
      .thenReturn(createPersonEntityList().filter { it.familyName == "Jones" })

    // When
    val personDetailsList = personRecordService.searchPersonRecords(searchRequest)

    // Then
    assertThat(personDetailsList).isNotEmpty().hasSize(2)
  }

  @Test
  fun `should return a single person record for an exact for provided search parameters`() {
    // Given
    val searchRequest = PersonSearchRequest(
      forename = "Randy",
      surname = "Jones",
      dateOfBirth = LocalDate.of(1969, 7, 30),
    )

    whenever(personRepository.searchByRequestParameters(searchRequest))
      .thenReturn(createPersonEntityList().filter { it.id == 4L })

    // When
    val personDetailsList = personRecordService.searchPersonRecords(searchRequest)

    // Then
    assertThat(personDetailsList).isNotEmpty().hasSize(1)
  }

  private fun createPersonEntityList(): List<PersonEntity> {
    return listOf(
      PersonEntity(
        id = 1L,
        personId = UUID.randomUUID(),
        givenName = "Bob",
        familyName = "Jones",
        dateOfBirth = LocalDate.now(),
      ),
      PersonEntity(
        id = 2L,
        personId = UUID.randomUUID(),
        givenName = "George",
        familyName = "Soros",
        dateOfBirth = LocalDate.now(),
      ),
      PersonEntity(
        id = 3L,
        personId = UUID.randomUUID(),
        givenName = "Rupert",
        familyName = "Murdoch",
        dateOfBirth = LocalDate.now(),
      ),
      PersonEntity(
        id = 4L,
        personId = UUID.randomUUID(),
        givenName = "Randy",
        familyName = "Jones",
        dateOfBirth = LocalDate.of(1969, 7, 30),
      ),
    )
  }
}
