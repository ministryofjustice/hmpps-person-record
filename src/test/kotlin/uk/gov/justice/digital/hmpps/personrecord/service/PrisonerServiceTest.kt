package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InOrder
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonServiceClient
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.PrisonerMatchCriteria
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Identifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerDetails
import uk.gov.justice.digital.hmpps.personrecord.config.FeatureFlag
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
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
  lateinit var prisonerSearchClient: PrisonerSearchClient

  @Mock
  lateinit var prisonServiceClient: PrisonServiceClient

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
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L"), prisonNumber = "12345"),
    )
    val personEntity = Person.from(person)

    val prisoner = Prisoner(
      firstName = "JOHN",
      lastName = "Doe",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "20230583843L",
    )

    val identifiers = listOf(Identifier(type = "PNC", value = "20230583843L"))

    val prisonerDetails = PrisonerDetails(
      firstName = "JOHN",
      lastName = "Doe",
      offenderNo = "12345",
      dateOfBirth = prisonerDOB,
      identifiers = identifiers,
    )

    whenever(prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person))).thenReturn(listOf(prisoner))
    whenever(prisonServiceClient.getPrisonerDetails(any())).thenReturn(prisonerDetails)
    whenever(prisonServiceClient.getPrisonerAddresses(any())).thenReturn(emptyList())

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    val inOrder: InOrder = inOrder(telemetryService, personRecordService)
    inOrder.verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "Prisoner Number" to prisoner.prisonerNumber,
      ),
    )
    inOrder.verify(personRecordService).addPrisonerToPerson(personEntity, prisonerDetails)
  }

  @Test
  fun `should track nomis pnc mismatch event`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("2001/0171310W")),
    )
    val personEntity = Person.from(person)
    val prisoner = Prisoner(
      firstName = "John",
      lastName = "Mahoney",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "20230583843L",
    )
    val prisonerList = listOf(prisoner)
    whenever(prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person))).thenReturn(prisonerList)

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
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L"), prisonNumber = "12345"),
    )
    val personEntity = Person.from(person)
    val prisoner = Prisoner(
      firstName = "John",
      lastName = "Mahoney",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "20230583843L",
    )
    whenever(prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person))).thenReturn(listOf(prisoner))

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
    val personEntity = PersonEntity.new()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = LocalDate.now(),
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L")),
    )
    whenever(prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person))).thenReturn(null)

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
    verifyNoInteractions(prisonerSearchClient)
    verifyNoInteractions(telemetryService)
  }

  @Test
  fun `should retry three times before returning prisoner matcher`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L"), prisonNumber = "12345"),
    )
    val personEntity = Person.from(person)

    val prisoner = Prisoner(
      firstName = "JOHN",
      lastName = "Doe",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "20230583843L",
    )

    // when
    whenever(
      prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person)),
    )
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(500)))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(500)))
      .thenReturn(listOf(prisoner))

    prisonerService.processAssociatedPrisoners(personEntity, person)

    // then
    verify(prisonerSearchClient, times(3)).findPossibleMatches(PrisonerMatchCriteria.from(person))
  }

  @Test
  fun `should retry before returning prisoner details`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L"), prisonNumber = "12345"),
    )
    val personEntity = Person.from(person)

    val prisoner = Prisoner(
      firstName = "JOHN",
      lastName = "Doe",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "20230583843L",
    )

    val identifiers = listOf(Identifier(type = "PNC", value = "20230583843L"))

    val prisonerDetails = PrisonerDetails(
      firstName = "JOHN",
      lastName = "Doe",
      offenderNo = "12345",
      dateOfBirth = prisonerDOB,
      identifiers = identifiers,
    )

    whenever(prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person))).thenReturn(listOf(prisoner))
    whenever(prisonServiceClient.getPrisonerDetails(any()))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(408)))
      .thenReturn(prisonerDetails)
    whenever(prisonServiceClient.getPrisonerAddresses(any()))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(408)))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(408)))
      .thenReturn(emptyList())

    // When
    prisonerService.processAssociatedPrisoners(personEntity, person)

    // Then
    verify(prisonServiceClient, times(2)).getPrisonerDetails(any())
    verify(prisonServiceClient, times(3)).getPrisonerAddresses(any())
  }

  @Test
  fun `should not retry when exception thrown is not in the list of exceptions for retry`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L"), prisonNumber = "12345"),
    )
    val personEntity = Person.from(person)

    // when
    whenever(
      prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person)),
    )
      .thenThrow(ResourceAccessException("socket timed out"))

    assertThrows<ResourceAccessException> { prisonerService.processAssociatedPrisoners(personEntity, person) }

    // then
    verify(prisonerSearchClient, times(1)).findPossibleMatches(PrisonerMatchCriteria.from(person))

    verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_CALL_FAILED,
      mapOf(
        "Prisoner Number" to person.otherIdentifiers?.prisonNumber,
      ),
    )
  }

  @Test
  fun `should retry three times and throw the exception and send correct telemetry event`() {
    // Given
    val prisonerDOB = LocalDate.now()
    val person = Person(
      givenName = "John",
      familyName = "Doe",
      dateOfBirth = prisonerDOB,
      otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("20230583843L"), prisonNumber = "12345"),
    )
    val personEntity = Person.from(person)

    val prisoner = Prisoner(
      firstName = "JOHN",
      lastName = "Doe",
      prisonerNumber = "12345",
      dateOfBirth = prisonerDOB,
      gender = "Male",
      nationality = "British",
      pncNumber = "20230583843L",
    )

    val identifiers = listOf(Identifier(type = "PNC", value = "20230583843L"))

    val prisonerDetails = PrisonerDetails(
      firstName = "JOHN",
      lastName = "Doe",
      offenderNo = "12345",
      dateOfBirth = prisonerDOB,
      identifiers = identifiers,
    )

    whenever(prisonerSearchClient.findPossibleMatches(PrisonerMatchCriteria.from(person))).thenReturn(listOf(prisoner))
    whenever(prisonServiceClient.getPrisonerDetails(any())).thenReturn(prisonerDetails)
    whenever(prisonServiceClient.getPrisonerAddresses(any()))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(408)))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(408)))
      .thenThrow(HttpServerErrorException(HttpStatusCode.valueOf(408)))

    // When
    assertThrows<HttpServerErrorException> { prisonerService.processAssociatedPrisoners(personEntity, person) }

    // Then
    verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_MATCH_FOUND,
      mapOf(
        "UUID" to personEntity.personId.toString(),
        "PNC" to person.otherIdentifiers?.pncIdentifier?.pncId,
        "Prisoner Number" to person.otherIdentifiers?.prisonNumber,
      ),
    )

    verify(telemetryService).trackEvent(
      TelemetryEventType.NOMIS_CALL_FAILED,
      mapOf(
        "Prisoner Number" to prisoner.prisonerNumber,
      ),
    )
  }
}
