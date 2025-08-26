package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
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
    assertThat(personMatchRecord.includeOverrideMarkers).isEmpty()
    assertThat(personMatchRecord.excludeOverrideMarkers).isEmpty()
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
    val include1 = OverrideMarkerEntity(person = personEntity, markerType = OverrideMarkerType.INCLUDE, markerValue = 111L)
    val include2 = OverrideMarkerEntity(person = personEntity, markerType = OverrideMarkerType.INCLUDE, markerValue = 222L)
    val exclude1 = OverrideMarkerEntity(person = personEntity, markerType = OverrideMarkerType.EXCLUDE, markerValue = 999L)

    personEntity.overrideMarkers.addAll(listOf(include1, include2, exclude1))

    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.dateOfBirth).isEqualTo("1970-01-01")
    assertThat(personMatchRecord.dateOfBirthAliases).isEqualTo(listOf("1970-01-01"))
    assertThat(personMatchRecord.sentenceDates).isEqualTo(listOf("1970-01-01"))
    assertThat(personMatchRecord.includeOverrideMarkers).containsExactly("111", "222")
    assertThat(personMatchRecord.excludeOverrideMarkers).containsExactly("999")
  }
}
