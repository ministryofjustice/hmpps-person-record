package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest
import java.time.LocalDate
import java.util.*
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class PersonRecordServiceTest {

  @Mock
  lateinit var personRepository: PersonRepository

  @Mock
  lateinit var offenderRepository: OffenderRepository

  @Mock
  lateinit var offenderSearchClient: ProbationOffenderSearchClient

  @Mock
  lateinit var defendantRepository: DefendantRepository

  @InjectMocks
  lateinit var personRecordService: PersonRecordService

  private lateinit var personEntity: PersonEntity
  private lateinit var dateOfBirth: LocalDate

  @BeforeEach
  fun setUp() {
    dateOfBirth = LocalDate.of(1969, 8, 20)
    personEntity = PersonEntity(
      id = 23232L,
      personId = UUID.fromString("f4165b62-d9eb-11ed-afa1-0242ac120002"),
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
    assertThat(personDTO.personId).isEqualTo(UUID.fromString("f4165b62-d9eb-11ed-afa1-0242ac120002"))
  }

  @Test
  fun `should throw exception when person does not exist for supplied id`() {
    // Given
    val uuid = UUID.randomUUID()

    // When
    val exception = assertFailsWith<EntityNotFoundException>(
      block = { personRecordService.getPersonById(uuid) },
    )

    // Then
    assertThat(exception.message).isEqualTo("Person record not found for id: $uuid")
  }

  @Test
  fun `should create a person record with unique person Identifier from supplied person dto`() {
    // Given
    val person = Person(
      givenName = "Stephen",
      middleNames = listOf("Michael", "James"),
      familyName = "Jones",
      dateOfBirth = LocalDate.of(1968, 8, 15),
      defendantId = "c04d3d2d-4bd2-40b9-bda6-564a4d9adb91",
    )

    whenever(personRepository.save(any())).thenReturn(personEntity)

    // When
    val personRecord = personRecordService.createPersonRecord(person)

    // Then
    verify(personRepository).save(any<PersonEntity>())
    assertThat(personRecord.personId).isNotNull
  }

  @Test
  fun `should create a person record with unique person Identifier from supplied person dto contains crn`() {
    // Given
    val person = Person(
      givenName = "Stephen",
      middleNames = listOf("Michael", "James"),
      familyName = "Jones",
      dateOfBirth = LocalDate.of(1968, 8, 15),
      defendantId = "c04d3d2d-4bd2-40b9-bda6-564a4d9adb91",
      otherIdentifiers = OtherIdentifiers(crn = "59770/20X"),
    )

    whenever(personRepository.save(any())).thenReturn(personEntity)

    // When
    val personRecord = personRecordService.createPersonRecord(person)

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
    val personList = personRecordService.searchPersonRecords(searchRequest)

    // Then
    assertThat(personList).isEmpty()
  }

  @Test
  fun `should return a single person record for an exact match for provided search parameters`() {
    // Given
    val searchRequest = PersonSearchRequest(
      forenameOne = "Randy",
      surname = "Jones",
      dateOfBirth = LocalDate.of(1969, 7, 30),
    )

    whenever(personRepository.searchByRequestParameters(searchRequest))
      .thenReturn(createPersonEntityList().filter { it.id == 4L })

    // When
    val personList = personRecordService.searchPersonRecords(searchRequest)

    // Then
    assertThat(personList).isNotEmpty().hasSize(1)
  }

  @Test
  fun `should search delius offender records when crn is provided`() {
    // Given
    val searchRequest = PersonSearchRequest(crn = "CRN1234")

    whenever(personRepository.findByOffendersCrn(any()))
      .thenReturn(personEntity)

    // When
    personRecordService.searchPersonRecords(searchRequest)

    // Then
    verify(personRepository).findByOffendersCrn(eq("CRN1234"))
    verify(personRepository, never()).searchByRequestParameters(any())
  }

  @Test
  fun `should throw exception when person does not exist for supplied CRN`() {
    // Given
    val searchRequest = PersonSearchRequest(crn = "CRN1234")
    whenever(personRepository.findByOffendersCrn(any()))
      .thenThrow(EntityNotFoundException("Person record not found for CRN"))

    // When
    val exception = assertFailsWith<EntityNotFoundException>(
      block = { personRecordService.searchPersonRecords(searchRequest) },
    )

    // Then
    assertThat(exception.message).isEqualTo("Person record not found for CRN")
    verify(personRepository, never()).searchByRequestParameters(any())
  }

  @Test
  fun `should throw validation exception when minimum search criteria are not provided`() {
    // Given
    val searchRequest = PersonSearchRequest(forenameOne = "Crispin")

    // When
    val exception = assertFailsWith<ValidationException>(
      block = { personRecordService.searchPersonRecords(searchRequest) },
    )

    assertThat(exception.message).isEqualTo("Surname not provided in search request")
    verify(personRepository, never()).searchByRequestParameters(any())
  }

  @Test
  fun `should only search by request parameters when crn is NOT provided`() {
    // Given
    val searchRequest = PersonSearchRequest(
      forenameOne = "Randy",
      surname = "Jones",
      dateOfBirth = LocalDate.of(1969, 7, 30),
    )

    whenever(personRepository.searchByRequestParameters(searchRequest))
      .thenReturn(createPersonEntityList().filter { it.id == 4L })

    // When
    personRecordService.searchPersonRecords(searchRequest)

    // Then
    verify(personRepository, never()).findByOffendersCrn(any())
    verify(personRepository).searchByRequestParameters(eq(searchRequest))
  }

  private fun createPersonEntityList(): List<PersonEntity> {
    return listOf(
      PersonEntity(
        id = 1L,
        personId = UUID.randomUUID(),
      ),
      PersonEntity(
        id = 2L,
        personId = UUID.randomUUID(),
      ),
      PersonEntity(
        id = 3L,
        personId = UUID.randomUUID(),
      ),
      PersonEntity(
        id = 4L,
        personId = UUID.randomUUID(),
      ),
    )
  }
}
