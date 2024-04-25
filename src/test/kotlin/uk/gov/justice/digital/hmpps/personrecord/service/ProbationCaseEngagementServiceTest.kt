package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.IDs
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ProbationCaseEngagementServiceTest {

  private lateinit var probationCaseEngagementService: ProbationCaseEngagementService

  @Mock
  lateinit var offenderRepository: OffenderRepository

  @Mock
  lateinit var telemetryService: TelemetryService

  @BeforeEach
  fun setUp() {
    probationCaseEngagementService = ProbationCaseEngagementService(offenderRepository, telemetryService)
  }

  @Test
  fun `should add offender when offender doesn't exist with matching crn`() {
    val deliusOffenderDetail = DeliusOffenderDetail(
      dateOfBirth = LocalDate.now(),
      name = Name(
        forename = "forename",
        surname = "surname",
        otherNames = emptyList(),
      ),
      identifiers = Identifiers(
        crn = "CRN1234",
        pnc = "19790163001B",
      ),
    )
    val offenderEntity = OffenderEntity.from(
      OffenderDetail(
        offenderId = 1234567,
        dateOfBirth = LocalDate.now(),
        firstName = "Test",
        otherIds = IDs(
          crn = "CRN1234",
        ),
        surname = "test",
      ),
    )
    whenever(offenderRepository.findByCrn(any())).thenReturn(null)
    whenever(offenderRepository.saveAndFlush(any())).thenReturn(offenderEntity)

    probationCaseEngagementService.processNewOffender(deliusOffenderDetail)

    verify(offenderRepository).findByCrn("CRN1234")
    verify(telemetryService).trackEvent(
      TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC,
      mapOf(
        "PNC" to deliusOffenderDetail.identifiers.pnc,
        "CRN" to deliusOffenderDetail.identifiers.crn,
      ),
    )
  }

  @Test
  fun `should not create a new offender and person when no offender exist for crn`() {
    val deliusOffenderDetail = DeliusOffenderDetail(
      dateOfBirth = LocalDate.now(),
      name = Name(
        forename = "forename",
        surname = "surname",
        otherNames = emptyList(),
      ),
      identifiers = Identifiers(
        crn = "CRN1234",
        pnc = "19790163001B",
      ),
    )
    val offenderEntity = OffenderEntity.from(
      OffenderDetail(
        offenderId = 1234567,
        dateOfBirth = LocalDate.now(),
        firstName = "Test",
        otherIds = IDs(
          crn = "CRN1234",
        ),
        surname = "test",
      ),
    )
    whenever(offenderRepository.findByCrn(any())).thenReturn(offenderEntity)

    probationCaseEngagementService.processNewOffender(deliusOffenderDetail)

    verify(offenderRepository).findByCrn("CRN1234")
    verifyNoInteractions(telemetryService)
  }
}
