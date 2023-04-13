package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

@ExtendWith(MockitoExtension::class)
class PersonRecordServiceTest {

  @Mock
  lateinit var personRepository: PersonRepository

  @InjectMocks
  lateinit var personRecordService: PersonRecordService

  @Test
  fun `should return person dto for known person id`() {
    // Given
    val personId = UUID.fromString("f4165b62-d9eb-11ed-afa1-0242ac120002")
    val dateOfBirth = LocalDate.of(1969, 8, 20)
    val personEntity = PersonEntity(
      id = 23232L,
      givenName = "Stephen",
      middleNames = "Michael James",
      dateOfBirth = dateOfBirth,
      familyName = "Jones",
      crn = "CRN1234",
      pncNumber = "PNC82882",
    )

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

}
