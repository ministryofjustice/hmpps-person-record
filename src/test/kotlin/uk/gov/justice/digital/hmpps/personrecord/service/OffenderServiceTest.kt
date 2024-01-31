
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import uk.gov.justice.digital.hmpps.personrecord.client.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.IDs
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.OffenderService
import uk.gov.justice.digital.hmpps.personrecord.service.PersonRecordService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import java.time.LocalDate

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
@ExtendWith(MockitoExtension::class)
class OffenderServiceTest {

  @Mock
  private lateinit var telemetryService: TelemetryService

  @Mock
  private lateinit var personRecordService: PersonRecordService

  @Mock
  private lateinit var client: ProbationOffenderSearchClient

  @Mock
  private lateinit var featureFlag: FeatureFlag

  @InjectMocks
  private lateinit var offenderService: OffenderService

  @BeforeEach
  fun setUp() {
    offenderService = OffenderService(telemetryService, personRecordService, client, featureFlag)
    whenever(featureFlag.isDeliusSearchEnabled()).thenReturn(true)
  }

  companion object {
    const val PNC_ID = "2003/0062845E"
    const val CRN = "X026350"
  }

  @Test
  fun `should add multiple offenders to a person record for multiple matched delius offenders`() {
    // Given
    val personEntity = PersonEntity.new()
    val dateOfBirth = LocalDate.now()
    val person = createPerson(dateOfBirth, PNCIdentifier(PNC_ID), CRN)
    val offenderDetail = createOffenderDetail(dateOfBirth)
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(listOf(offenderDetail, offenderDetail, offenderDetail))

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verify(personRecordService, times(3)).addOffenderToPerson(any(), any())
    verify(telemetryService, times(3)).trackEvent(
      TelemetryEventType.DELIUS_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "CRN" to person.otherIdentifiers?.crn,
      ),
    )
  }

  @Test
  fun `should NOT add offenders to a person record for multiple unmatched delius offenders`() {
    // Given
    val personEntity = PersonEntity.new()
    val dateOfBirth = LocalDate.now()
    val person = createPerson(dateOfBirth, PNCIdentifier(PNC_ID), CRN)
    val offenderDetail = createOffenderDetail(LocalDate.of(1969, 8, 18))
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(listOf(offenderDetail, offenderDetail, offenderDetail))

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verifyNoInteractions(telemetryService)
  }

  @Test
  fun `should add offender to person record when exact match found`() {
    // Given
    val personEntity = PersonEntity.new()
    val dateOfBirth = LocalDate.now()
    val person = createPerson(dateOfBirth, PNCIdentifier(PNC_ID), CRN)
    val offenderDetail = createOffenderDetail(dateOfBirth)
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(listOf(offenderDetail))

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verify(personRecordService).addOffenderToPerson(personEntity, Person.from(offenderDetail))
    verify(telemetryService).trackEvent(
      TelemetryEventType.DELIUS_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "CRN" to person.otherIdentifiers?.crn,
      ),
    )
    verifyNoMoreInteractions(telemetryService)
  }

  private fun createOffenderDetail(dateOfBirth: LocalDate): OffenderDetail {
    val offenderDetail = OffenderDetail(
      offenderId = 1234L,
      firstName = "John",
      surname = "MAHONEY",
      dateOfBirth = dateOfBirth,
      otherIds = IDs(pncNumber = PNC_ID, crn = "crn1234"),
    )
    return offenderDetail
  }

  @Test
  fun `should track partial match event when partial match found`() {
    // Given
    val personEntity = PersonEntity.new()
    val person = createPerson(LocalDate.now(), PNCIdentifier(PNC_ID), CRN)
    val offenderDetail = OffenderDetail(
      offenderId = 1234L,
      firstName = "Frank",
      surname = "MAHONEY",
      dateOfBirth = LocalDate.of(1978, 4, 5),
      otherIds = IDs(pncNumber = PNC_ID, crn = CRN),
    )
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(listOf(offenderDetail))

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verify(telemetryService).trackEvent(
      TelemetryEventType.DELIUS_PARTIAL_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "CRN" to person.otherIdentifiers?.crn,
        ),
    )
  }

  @Test
  fun `should track no match event when no matching records exist`() {
    // Given
    val personEntity = PersonEntity.new()
    val person = createPerson(LocalDate.now(), PNCIdentifier(PNC_ID), " ")
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(emptyList())

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verify(telemetryService).trackEvent(
      TelemetryEventType.DELIUS_NO_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
      ),
    )
  }

  @Test
  fun `should not call delius search when feature flag is switched off`() {
    // Given
    val personEntity = PersonEntity.new()
    val person = createPerson(LocalDate.now(), PNCIdentifier(PNC_ID), "")

    whenever(featureFlag.isDeliusSearchEnabled()).thenReturn(false)
    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verifyNoInteractions(client)
    verifyNoInteractions(personRecordService)
    verifyNoInteractions(telemetryService)
  }

  @Test
  fun `should send correct parameters to the event when exact match is found with prison number`() {
    // Given
    val personEntity = PersonEntity.new()
    val dateOfBirth = LocalDate.now()
    val person = Person(
      givenName = "Frank",
      familyName = "MAHONEY",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier(PNC_ID), crn = CRN, prisonNumber = "A12324J"),
    )
    val offenderDetail = OffenderDetail(
      offenderId = 1234L,
      firstName = "Frank",
      surname = "MAHONEY",
      dateOfBirth = dateOfBirth,
      otherIds = IDs(pncNumber = PNC_ID, crn = CRN, nomsNumber = "A1234J"),
    )
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(listOf(offenderDetail))

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verify(personRecordService).addOffenderToPerson(personEntity, Person.from(offenderDetail))
    verify(telemetryService).trackEvent(
      TelemetryEventType.DELIUS_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "CRN" to person.otherIdentifiers?.crn,
        "PRISON NUMBER" to person.otherIdentifiers?.prisonNumber
      ),
    )
  }

  private fun createPerson(dateOfBirth: LocalDate?, pncIdentifier: PNCIdentifier, crn: String): Person {
    val person = Person(
      givenName = "John",
      familyName = "Mahoney",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = pncIdentifier, crn = crn),
    )
    return person
  }
}
