package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.time.LocalDate

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
@ExtendWith(MockitoExtension::class)
class CourtCaseEventsServiceTest {

  @Mock
  lateinit var telemetryService: TelemetryService

  @Mock
  lateinit var personRecordService: PersonRecordService

  @Mock
  lateinit var offenderService: OffenderService

  @Mock
  lateinit var prisonerService: PrisonerService

  @Mock
  lateinit var defendantRepository: DefendantRepository

  @InjectMocks
  lateinit var courtCaseEventsService: CourtCaseEventsService

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
