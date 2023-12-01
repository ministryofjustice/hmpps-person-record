package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdValidator
import java.time.LocalDate

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
@ExtendWith(MockitoExtension::class)
class CourtCaseEventsServiceTest {

  @Mock
  lateinit var telemetryService: TelemetryService

  @Mock
  lateinit var pncIdValidator: PNCIdValidator

  @Mock
  lateinit var defendantRepository: DefendantRepository

  @InjectMocks
  lateinit var courtCaseEventsService: CourtCaseEventsService

  @BeforeEach
  fun setUp() {
//    whenever(pncIdValidator.isValid(anyString())).thenReturn(true)
  }

  @Test
  fun `should call telemetry service when PNC is missing from Court Case Event`() {
    // Given
    val person = Person(familyName = "Jones")

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
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
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_INVALID_PNC, mapOf("PNC" to pncNumber))
  }

  @Test
  fun `should call telemetry service when exact match found for Court Case Event`() {
    // Given
    val pncNumber = "PNC12345"
    val dateOfBirth = LocalDate.now()
    whenever(pncIdValidator.isValid(anyString())).thenReturn(true)
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
      givenName = "Crackity",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(pncNumber = pncNumber),
    )

    // When
    courtCaseEventsService.processPersonFromCourtCaseEvent(person)

    // Then
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_EXACT_MATCH, mapOf("PNC" to pncNumber))
  }

  @Test
  fun `should call telemetry service when partial match found for Court Case Event`() {
    // Given
    val pncNumber = "PNC12345"
    val dateOfBirth = LocalDate.now()
    whenever(pncIdValidator.isValid(anyString())).thenReturn(true)
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
    verify(telemetryService).trackEvent(TelemetryEventType.NEW_CASE_PARTIAL_MATCH, mapOf("Surname" to "Jones"))
  }
}
