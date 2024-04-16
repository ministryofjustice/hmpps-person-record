package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ProbationCaseEngagementServiceTest {

  lateinit var probationCaseEngagementService: ProbationCaseEngagementService

  @Mock
  lateinit var personRepository: PersonRepository

  @Mock
  lateinit var telemetryService: TelemetryService

  @BeforeEach
  fun setUp() {
    probationCaseEngagementService = ProbationCaseEngagementService(telemetryService, PersonRecordService(personRepository))
  }

  @Test
  fun `should add offender to person when person exist with matching pnc`() {
    val personEntity = PersonEntity()
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
    whenever(personRepository.findPersonEntityByPncNumber(any())).thenReturn(mutableListOf(personEntity))
    whenever(personRepository.saveAndFlush(any())).thenReturn(personEntity)

    probationCaseEngagementService.processNewOffender(deliusOffenderDetail)

    verify(personRepository).findPersonEntityByPncNumber(PNCIdentifier.from("19790163001B"))
    verify(personRepository).saveAndFlush(personEntity)
    verify(telemetryService).trackEvent(
      TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED,
      mapOf(
        "PNC" to deliusOffenderDetail.identifiers.pnc,
        "CRN" to deliusOffenderDetail.identifiers.crn,
      ),
    )
  }

  @Test
  fun `should create a new offender and person when no person exist for pnc`() {
    val personEntity = PersonEntity()
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
    whenever(personRepository.findPersonEntityByPncNumber(any())).thenReturn(emptyList())
    whenever(personRepository.saveAndFlush(any())).thenReturn(personEntity)

    probationCaseEngagementService.processNewOffender(deliusOffenderDetail)

    verify(personRepository).findPersonEntityByPncNumber(PNCIdentifier.from("19790163001B"))
    verify(telemetryService).trackEvent(
      TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to deliusOffenderDetail.identifiers.pnc,
        "CRN" to deliusOffenderDetail.identifiers.crn,
      ),
    )
  }
}
