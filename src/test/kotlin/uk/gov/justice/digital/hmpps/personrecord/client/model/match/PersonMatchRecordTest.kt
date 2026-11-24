package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.OverrideType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

class PersonMatchRecordTest {

  @Test
  fun `should build dates in correct YYYY-MM-dd format`() {
    val date = LocalDate.of(1970, 1, 1)
    val personEntity = PersonEntity(
      pseudonyms = mutableListOf(
        PseudonymEntity(nameType = NameType.ALIAS, dateOfBirth = date),
        PseudonymEntity(nameType = NameType.PRIMARY, dateOfBirth = date),
      ),

      sentenceInfo = mutableListOf(SentenceInfoEntity(sentenceDate = date)),
      matchId = UUID.randomUUID(),
      sourceSystem = DELIUS,
    )

    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.dateOfBirth).isEqualTo("1970-01-01")
    assertThat(personMatchRecord.dateOfBirthAliases).isEqualTo(listOf("1970-01-01"))
    assertThat(personMatchRecord.sentenceDates).isEqualTo(listOf("1970-01-01"))
  }

  @ParameterizedTest(name = "{0}", quoteTextArguments = false)
  @MethodSource("pncAndCroValues")
  fun `should send PNCs and CROs in canonical form and drop invalid values`(
    description: String,
    identifierType: IdentifierType,
    storedValues: List<String>,
    expected: List<String>,
  ) {
    val personEntity = PersonEntity(
      references = storedValues.map { ReferenceEntity(identifierType = identifierType, identifierValue = it) }.toMutableList(),
      matchId = UUID.randomUUID(),
      sourceSystem = DELIUS,
    )

    val personMatchRecord = PersonMatchRecord.from(personEntity)
    val actual = if (identifierType == PNC) personMatchRecord.pncs else personMatchRecord.cros
    assertThat(actual).containsExactlyElementsOf(expected)
  }

  // TODO remove this test when RemandOverrideMarkersE2ETest.kt:54 is enabled
  /** See [RemandOverrideMarkersE2ETest] */
  @Test
  fun `should build override marker and override scopes`() {
    val overrideMarker = UUID.randomUUID()
    val overrideScope1 = UUID.randomUUID()
    val overrideScope2 = UUID.randomUUID()
    val personEntity = PersonEntity(
      pseudonyms = mutableListOf(
        PseudonymEntity(nameType = NameType.PRIMARY, dateOfBirth = randomDate()),
      ),

      matchId = UUID.randomUUID(),
      sourceSystem = DELIUS,
      overrideMarker = overrideMarker,
      overrideScopes = mutableListOf(
        OverrideScopeEntity(scope = overrideScope1, actor = ActorType.HUMAN, confidence = ConfidenceType.VERIFIED, overrideType = OverrideType.EXCLUDE),
        OverrideScopeEntity(scope = overrideScope2, actor = ActorType.SYSTEM, confidence = ConfidenceType.VERIFIED, overrideType = OverrideType.EXCLUDE),
      ),

    )

    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.overrideMarker).isEqualTo(overrideMarker.toString())
    assertThat(personMatchRecord.overrideScopes).isEqualTo(listOf(overrideScope1.toString(), overrideScope2.toString()))
  }

  companion object {
    @JvmStatic
    fun pncAndCroValues(): Stream<Arguments> {
      val shortPnc = randomShortPnc()
      val longPnc = PNCIdentifier.from(shortPnc).pncId
      val cro = randomCro()
      return Stream.of(
        Arguments.of("short PNC is sent in long form", PNC, listOf(shortPnc), listOf(longPnc)),
        Arguments.of("long PNC is sent unchanged", PNC, listOf(longPnc), listOf(longPnc)),
        Arguments.of("short and long form of the same PNC are sent once", PNC, listOf(shortPnc, longPnc), listOf(longPnc)),
        Arguments.of("invalid PNC is dropped", PNC, listOf("1231/XX/121"), emptyList<String>()),
        Arguments.of("valid CRO is sent unchanged", CRO, listOf(cro), listOf(cro)),
        Arguments.of("invalid CRO is dropped", CRO, listOf("123456/08X"), emptyList<String>()),
      )
    }
  }
}
