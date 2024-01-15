package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.time.LocalDate
import java.util.*

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
@ExtendWith(MockitoExtension::class)
class CourtCaseEventsServiceTest {

  @Mock
  lateinit var telemetryService: TelemetryService

  @Mock
  lateinit var personRecordService: PersonRecordService

  @Mock
  lateinit var prisonerService: PrisonerService

  @Mock
  lateinit var offenderService: OffenderService

  @Mock
  lateinit var defendantRepository: DefendantRepository

  @InjectMocks
  lateinit var courtCaseEventsService: CourtCaseEventsService

  @Test
  fun `should call telemetry service when PNC is missing from Court Case Event`() {
    // Given
    val person = Person(familyName = "Jones")

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createDefendantFromPerson(person)
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_MISSING_PNC, emptyMap())
  }

  @Test
  fun `should call telemetry service when PNC is invalid from Court Case Event`() {
    // Given
    val pncNumber = "DODGY_PNC"
    val person = Person(familyName = "Jones", otherIdentifiers = OtherIdentifiers(pncNumber = pncNumber))

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createDefendantFromPerson(person)
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_INVALID_PNC, mapOf("PNC" to pncNumber))
  }

  @Test
  fun `should call telemetry service when exact match found for Court Case Event`() {
    // Given
    val pncNumber = "20030011985X"
    val crn = "CRN123"
    val personID = UUID.fromString("2936dd6a-677a-4cc0-83c5-2296b6efee0b")
    val dateOfBirth = LocalDate.now()

    whenever(defendantRepository.findAllByPncNumber(pncNumber))
      .thenReturn(
        listOf(
          DefendantEntity(
            pncNumber = pncNumber,
            crn = crn,
            surname = "Jones",
            forenameOne = "Crackity",
            dateOfBirth = dateOfBirth,
            defendantId = "122434",
          ),
        ),
      )

    val person = Person(
      personId = personID,
      familyName = "Jones",
      givenName = "Crackity",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(pncNumber = pncNumber),
    )

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createDefendantFromPerson(person)
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_EXACT_MATCH, mapOf("PNC" to pncNumber, "CRN" to crn, "UUID" to personID.toString()))
  }

  @Test
  fun `should call telemetry service when partial match found for Court Case Event`() {
    // Given
    val pncNumber = "20030011985X"
    val dateOfBirth = LocalDate.now()

    whenever(defendantRepository.findAllByPncNumber(pncNumber))
      .thenReturn(
        listOf(
          DefendantEntity(
            pncNumber = pncNumber,
            surname = "Jones",
            forenameOne = "Crackity",
            dateOfBirth = dateOfBirth,
            defendantId = "122434",
          ),
        ),
      )

    val person = Person(
      familyName = "Jones",
      givenName = "Billy",
      dateOfBirth = LocalDate.of(1969, 8, 15),
      otherIdentifiers = OtherIdentifiers(pncNumber = pncNumber),
    )

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createDefendantFromPerson(person)
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_PARTIAL_MATCH, mapOf("Surname" to "Jones"))
  }

  @Test
  fun `should create new defendant record when no matching records are found`() {
    // Given
    val pncNumber = "20030011985X"

    val person = Person(
      familyName = "Jones",
      givenName = "Billy",
      dateOfBirth = LocalDate.of(1969, 8, 15),
      otherIdentifiers = OtherIdentifiers(pncNumber = pncNumber),
    )

    val uuid = UUID.randomUUID()
    val personEntity = Person.from(person.copy(personId = uuid))
    whenever(personRecordService.createDefendantFromPerson(person)).thenReturn(personEntity)

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService).createDefendantFromPerson(person)
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_PERSON_CREATED, mapOf("UUID" to uuid.toString(), "PNC" to pncNumber))
    verify(offenderService).processAssociatedOffenders(personEntity, person)
    verify(prisonerService).processAssociatedPrisoners(personEntity, person)
  }

  @Test
  fun `should not create new defendant when mulitple existing records are found`() {
    // Given
    val pncNumber = "20030011985X"

    val person = Person(
      familyName = "Jones",
      givenName = "Billy",
      dateOfBirth = LocalDate.of(1969, 8, 15),
      otherIdentifiers = OtherIdentifiers(pncNumber = pncNumber),
    )

    val defendant = DefendantEntity()
    whenever(defendantRepository.findAllByPncNumber(pncNumber)).thenReturn(listOf(defendant, defendant))

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verifyNoInteractions(personRecordService)
    verifyNoInteractions(offenderService)
    verifyNoInteractions(prisonerService)
  }
}
