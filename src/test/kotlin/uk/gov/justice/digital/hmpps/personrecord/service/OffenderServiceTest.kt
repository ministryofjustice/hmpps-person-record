
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
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

  @Test
  fun `should add offender to person record when exact match found`() {
    // Given
    val personEntity = PersonEntity()
    val dateOfBirth = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Mahoney",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(pncNumber = "PNC123"),
    )
    val offenderDetail = OffenderDetail(
      offenderId = 1234L,
      firstName = "John",
      surname = "MAHONEY",
      dateOfBirth = dateOfBirth,
      otherIds = IDs(pncNumber = "PNC123", crn = "crn1234"),
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
        "PNC" to person.otherIdentifiers?.pncNumber,
        "CRN" to person.otherIdentifiers?.crn,
      ),
    )
  }

  @Test
  fun `should track partial match event when partial match found`() {
    // Given
    val personEntity = PersonEntity()
    val person = Person(
      givenName = "John",
      familyName = "Mahoney",
      dateOfBirth = LocalDate.now(),
      otherIdentifiers = OtherIdentifiers(pncNumber = "PNC123"),
    )
    val offenderDetail = OffenderDetail(
      offenderId = 1234L,
      firstName = "Frank",
      surname = "MAHONEY",
      dateOfBirth = LocalDate.of(1978, 4, 5),
      otherIds = IDs(pncNumber = "PNC123", crn = "crn1234"),
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
        "PNC" to person.otherIdentifiers?.pncNumber,
        "CRN" to person.otherIdentifiers?.crn,
      ),
    )
  }

  @Test
  fun `should track no match event when no matching records exist`() {
    // Given
    val personEntity = PersonEntity()
    val person = Person(
      givenName = "John",
      familyName = "Mahoney",
      dateOfBirth = LocalDate.now(),
      otherIdentifiers = OtherIdentifiers(pncNumber = "PNC123"),
    )
    whenever(client.getOffenderDetail(SearchDto.from(person))).thenReturn(emptyList())

    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verifyNoInteractions(personRecordService)
    verify(telemetryService).trackEvent(
      TelemetryEventType.DELIUS_NO_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncNumber,
      ),
    )
  }

  @Test
  fun `should not call delius search when feature flag is switched off`() {
    // Given
    val personEntity = PersonEntity()
    val dateOfBirth = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Mahoney",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(pncNumber = "PNC123"),
    )
    val offenderDetail = OffenderDetail(
      offenderId = 1234L,
      firstName = "John",
      surname = "MAHONEY",
      dateOfBirth = dateOfBirth,
      otherIds = IDs(pncNumber = "PNC123", crn = "crn1234"),
    )
    whenever(featureFlag.isDeliusSearchEnabled()).thenReturn(false)
    // When
    offenderService.processAssociatedOffenders(personEntity, person)

    // Then
    verifyNoInteractions(client)
    verifyNoInteractions(personRecordService)
    verifyNoInteractions(telemetryService)
  }
}
