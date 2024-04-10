package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.HMCTS_EXACT_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.INVALID_PNC
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.VALID_PNC
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
    verify(personRecordService, never()).createNewPersonAndDefendant(person)
    verify(telemetryService).trackEvent(TelemetryEventType.MISSING_PNC, emptyMap())
    verifyNoMoreInteractions(telemetryService)
  }

  @Test
  fun `should call telemetry service when PNC is invalid from Court Case Event`() {
    // Given
    val pncNumber = "DODGY_PNC"
    val person = Person(familyName = "Jones", otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(pncNumber)))

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createNewPersonAndDefendant(person)
    verify(telemetryService).trackEvent(INVALID_PNC, mapOf("PNC" to pncNumber))
    verifyNoMoreInteractions(telemetryService)
  }

  @Test
  fun `should call telemetry service when exact match found for Court Case Event`() {
    // Given
    val pncNumber = "2003/0011985X"
    val crn = "CRN123"
    val personID = UUID.fromString("2936dd6a-677a-4cc0-83c5-2296b6efee0b")
    val dateOfBirth = LocalDate.now()
    whenever(defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber)))
      .thenReturn(
        mutableListOf(
          DefendantEntity(
            pncNumber = PNCIdentifier.from(pncNumber),
            crn = crn,
            surname = "Jones",
            firstName = "Crackity",
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
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(pncNumber)),
    )

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createNewPersonAndDefendant(person)
    verify(telemetryService).trackEvent(VALID_PNC, mapOf("PNC" to pncNumber))
    verify(telemetryService).trackEvent(HMCTS_EXACT_MATCH, mapOf("PNC" to pncNumber, "CRN" to crn, "UUID" to personID.toString()))
  }

  @Test
  fun `should call telemetry service when partial match found for Court Case Event`() {
    // Given
    val pncNumber = "2003/0011985X"
    val dateOfBirth = LocalDate.now()
    whenever(defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber)))
      .thenReturn(
        mutableListOf(
          DefendantEntity(
            pncNumber = PNCIdentifier.from(pncNumber),
            surname = "Jones",
            firstName = "Crackity",
            dateOfBirth = dateOfBirth,
            defendantId = "122434",
          ),
        ),
      )

    val person = Person(
      familyName = "Jones",
      givenName = "Billy",
      dateOfBirth = LocalDate.of(1969, 8, 15),
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from(pncNumber)),
    )

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(personRecordService, never()).createNewPersonAndDefendant(person)
    verify(telemetryService).trackEvent(TelemetryEventType.HMCTS_PARTIAL_MATCH, mapOf("Surname" to "Jones"))
  }
}
