package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InOrder
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PossibleMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PrisonerServiceTest {

  @InjectMocks
  lateinit var prisonerService: PrisonerService

  @Mock
  lateinit var telemetryService: TelemetryService

  @Mock
  lateinit var personRecordService: PersonRecordService

  @Mock
  lateinit var client: PrisonerSearchClient

  @Mock
  lateinit var featureFlag: FeatureFlag

  @BeforeEach
  fun setUp() {
    whenever(featureFlag.isNomisSearchEnabled()).thenReturn(true)
  }

  @Test
  fun `should add prisoner to person record when exact match found associated prisoners correctly`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier("PNC123")),
    )
    val personEntity = Person.from(person)

    val prisoner = Prisoner(
      firstName = "John",
      lastName = "Doe",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "PNC123",
    )

    whenever(client.findPossibleMatches(PossibleMatchCriteria.from(person))).thenReturn(listOf(prisoner))

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    val inOrder: InOrder = inOrder(telemetryService, personRecordService)
    inOrder.verify(personRecordService).addPrisonerToPerson(personEntity, prisoner)
    inOrder.verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "Prisoner Number" to prisoner.prisonerNumber,
      ),
    )
  }

  @Test
  fun `should track nomis pnc mismatch event`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier("PNC123")),
    )
    val personEntity = Person.from(person)
    val prisoner = Prisoner(
      firstName = "John",
      lastName = "Mahoney",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "PNC999",
    )
    val prisonerList = listOf(prisoner)
    whenever(client.findPossibleMatches(PossibleMatchCriteria.from(person))).thenReturn(prisonerList)

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_PNC_MISMATCH,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC searched for" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "PNC returned from search" to prisoner.pncNumber,
        "Prisoner Number" to prisonerList[0].prisonerNumber,
      ),
    )
  }

  @Test
  fun `should track partial match event when partial match found`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier("PNC123")),
    )
    val personEntity = Person.from(person)
    val prisoner = Prisoner(
      firstName = "John",
      lastName = "Mahoney",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "PNC123",
    )
    whenever(client.findPossibleMatches(PossibleMatchCriteria.from(person))).thenReturn(listOf(prisoner))

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_PARTIAL_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "Prisoner Number" to prisoner.prisonerNumber,
      ),
    )
  }

  @Test
  fun `should not call person record service when no nomis results are returned`() {
    // Given
    val searchCriteria = PossibleMatchCriteria()
    whenever(client.findPossibleMatches(searchCriteria)).thenReturn(null)
    val personEntity = PersonEntity()
    val person = Person()

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_NO_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
      ),
    )
  }

  @Test
  fun `should not process associated prisoners when nomis feature flag is set to false`() {
    // Given
    whenever(featureFlag.isNomisSearchEnabled()).thenReturn(false)
    val person = Person()
    val personEntity = Person.from(person)

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verifyNoInteractions(client)
    verifyNoInteractions(telemetryService)
  }
}
