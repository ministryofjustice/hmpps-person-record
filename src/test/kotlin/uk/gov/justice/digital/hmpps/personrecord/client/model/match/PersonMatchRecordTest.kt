package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import java.time.LocalDate
import java.util.UUID

class PersonMatchRecordTest {

  @Test
  fun `should return empty strings when building request if is null`() {
    val personEntity = PersonEntity(
      matchId = UUID.randomUUID(),
      sourceSystem = DELIUS,
    )
    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.firstName).isEmpty()
    assertThat(personMatchRecord.middleNames).isEmpty()
    assertThat(personMatchRecord.lastName).isEmpty()
    assertThat(personMatchRecord.dateOfBirth).isEmpty()
    assertThat(personMatchRecord.firstNameAliases).isEmpty()
    assertThat(personMatchRecord.lastNameAliases).isEmpty()
    assertThat(personMatchRecord.dateOfBirthAliases).isEmpty()
    assertThat(personMatchRecord.postcodes).isEmpty()
    assertThat(personMatchRecord.cros).isEmpty()
    assertThat(personMatchRecord.pncs).isEmpty()
    assertThat(personMatchRecord.sentenceDates).isEmpty()
  }

  @Test
  fun `should build dates in correct YYYY-MM-dd format`() {
    val date = LocalDate.of(1970, 1, 1)
    val personEntity = PersonEntity(
      dateOfBirth = date,
      pseudonyms = mutableListOf(PseudonymEntity(type = NameType.ALIAS, dateOfBirth = date)),
      sentenceInfo = mutableListOf(SentenceInfoEntity(sentenceDate = date)),
      matchId = UUID.randomUUID(),
      sourceSystem = DELIUS,
    )
    val personMatchRecord = PersonMatchRecord.from(personEntity)
    assertThat(personMatchRecord.dateOfBirth).isEqualTo("1970-01-01")
    assertThat(personMatchRecord.dateOfBirthAliases).isEqualTo(listOf("1970-01-01"))
    assertThat(personMatchRecord.sentenceDates).isEqualTo(listOf("1970-01-01"))
  }
}
