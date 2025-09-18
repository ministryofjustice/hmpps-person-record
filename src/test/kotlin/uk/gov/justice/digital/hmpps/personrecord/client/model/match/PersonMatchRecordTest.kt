package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import java.time.LocalDate
import java.util.UUID

class PersonMatchRecordTest {

  @Test
  fun `should return empty strings when building request if is null`() {
    val personEntity = PersonEntity(
      matchId = UUID.randomUUID(),
      sourceSystem = DELIUS,
      pseudonyms = mutableListOf(PseudonymEntity(nameType = NameType.PRIMARY, lastName = "MANDATORY FIELD")),
    )
    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.firstName).isEmpty()
    assertThat(personMatchRecord.middleNames).isEmpty()
    assertThat(personMatchRecord.dateOfBirth).isEmpty()
    assertThat(personMatchRecord.firstNameAliases).isEmpty()
    assertThat(personMatchRecord.lastNameAliases).isEmpty()
    assertThat(personMatchRecord.dateOfBirthAliases).isEmpty()
    assertThat(personMatchRecord.postcodes).isEmpty()
    assertThat(personMatchRecord.cros).isEmpty()
    assertThat(personMatchRecord.pncs).isEmpty()
    assertThat(personMatchRecord.sentenceDates).isEmpty()
    assertThat(personMatchRecord.overrideMarker).isEmpty()
    assertThat(personMatchRecord.overrideScopes).isEmpty()
  }

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
        OverrideScopeEntity(scope = overrideScope1, actor = ActorType.HUMAN, confidence = ConfidenceType.VERIFIED),
        OverrideScopeEntity(scope = overrideScope2, actor = ActorType.SYSTEM, confidence = ConfidenceType.VERIFIED),
      ),

    )

    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.overrideMarker).isEqualTo(overrideMarker.toString())
    assertThat(personMatchRecord.overrideScopes).isEqualTo(listOf(overrideScope1.toString(), overrideScope2.toString()))
  }
}
