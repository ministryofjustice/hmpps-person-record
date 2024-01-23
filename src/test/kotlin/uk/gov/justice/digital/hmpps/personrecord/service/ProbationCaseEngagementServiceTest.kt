package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.Name
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.time.LocalDate

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
@ExtendWith(MockitoExtension::class)
class ProbationCaseEngagementServiceTest {
  @InjectMocks
  lateinit var probationCaseEngagementService: ProbationCaseEngagementService

  @Mock
  lateinit var personRepository: PersonRepository

  @Mock
  lateinit var telemetryService: TelemetryService

  @BeforeEach
  fun setUp() {
    probationCaseEngagementService = ProbationCaseEngagementService(personRepository, telemetryService)
  }

  @Test
  fun `should add offender to person when person exist with same pnc`() {
    val personEntity = PersonEntity()
    val deliusOffenderDetail = DeliusOffenderDetail(
      dateOfBirth = LocalDate.now(),
      name = Name(
        forename = "forname",
        surname = "surname",
        otherNames = emptyList(),
      ),
      identifiers = Identifiers(
        crn = "CRN1234",
        pnc = "19790163001B",
      ),
    )
    whenever(personRepository.findPersonEntityByPncNumber(any())).thenReturn(personEntity)
    whenever(personRepository.saveAndFlush(any())).thenReturn(personEntity)

    probationCaseEngagementService.processNewOffender(deliusOffenderDetail)

    verify(personRepository).findPersonEntityByPncNumber("19790163001B")
    verify(personRepository).saveAndFlush(personEntity)
    verify(telemetryService).trackEvent(
      TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED,
      mapOf(
        "PNC" to deliusOffenderDetail.identifiers.pnc,
        "CRN" to deliusOffenderDetail.identifiers.crn,
      ),
    )
  }
}
