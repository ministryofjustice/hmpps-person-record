package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class PrisonReligionGetResponseBodyTest {

  @Test
  fun `should build object correctly`() {
    val prisonNumber = randomPrisonNumber()
    val prisonReligionEntity = PrisonReligionEntity(
      id = 1,
      updateId = UUID.randomUUID(),
      prisonNumber = prisonNumber,
      code = randomPrisonNumber(),
      changeReasonKnown = randomBoolean(),
      comments = randomLowerCaseString(),
      verified = randomBoolean(),
      startDate = randomDate(),
      endDate = randomDate(),
      modifyDateTime = randomDateTime(),
      modifyUserId = randomLowerCaseString(),
      prisonRecordType = PrisonRecordType.entries.random(),
    )

    val actual = PrisonReligionGetResponseBody.from(prisonNumber, prisonReligionEntity)

    val expected = PrisonReligionGetResponseBody(
      prisonNumber = prisonNumber,
      religion = PrisonReligion(
        religionCode = prisonReligionEntity.code,
        changeReasonKnown = prisonReligionEntity.changeReasonKnown,
        comments = prisonReligionEntity.comments,
        verified = prisonReligionEntity.verified,
        startDate = prisonReligionEntity.startDate,
        endDate = prisonReligionEntity.endDate,
        modifyDateTime = prisonReligionEntity.modifyDateTime,
        modifyUserId = prisonReligionEntity.modifyUserId,
        current = prisonReligionEntity.prisonRecordType.value,
      ),
    )
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
  }
}
