package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import java.util.UUID

class PrisonReligionGetResponseTest {

  @Test
  fun `should build object correctly`() {
    val prisonNumber = randomPrisonNumber()
    val prisonReligionEntity = PrisonReligionEntity(
      id = 1,
      updateId = UUID.randomUUID(),
      prisonNumber = prisonNumber,
      code = randomReligionCode(),
      changeReasonKnown = randomBoolean(),
      comments = randomLowerCaseString(),
      startDate = randomDate(),
      endDate = randomDate(),
      modifyDateTime = randomDateTime(),
      modifyUserId = randomLowerCaseString(),
      prisonRecordType = PrisonRecordType.entries.random(),
      createDateTime = randomDateTime(),
      createUserId = randomLowerCaseString(),
    )

    val actual = PrisonReligionReadResponse.from(prisonNumber, prisonReligionEntity)

    val expected = PrisonReligionReadResponse(
      prisonNumber = prisonNumber,
      religion = PrisonReligion(
        religionCode = prisonReligionEntity.code,
        religionDescription = ReligionCode.valueOf(prisonReligionEntity.code!!).description,
        changeReasonKnown = prisonReligionEntity.changeReasonKnown,
        comments = prisonReligionEntity.comments,
        startDate = prisonReligionEntity.startDate,
        endDate = prisonReligionEntity.endDate,
        modifyDateTime = prisonReligionEntity.modifyDateTime,
        modifyUserId = prisonReligionEntity.modifyUserId,
        current = prisonReligionEntity.prisonRecordType.value,
        createDateTime = prisonReligionEntity.createDateTime,
        createUserId = prisonReligionEntity.createUserId,
      ),
    )
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
  }
}
