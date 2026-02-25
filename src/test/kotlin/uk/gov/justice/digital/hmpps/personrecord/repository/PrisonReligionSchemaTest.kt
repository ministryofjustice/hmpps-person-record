package uk.gov.justice.digital.hmpps.personrecord.repository

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode

class PrisonReligionSchemaTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Test
  fun `should fail to insert multiple current religions`() {
    val prisonNumber = randomPrisonNumber()
    val religion = PrisonReligionEntity.from(prisonNumber, createRandomCurrentReligion())
    val religion2 = PrisonReligionEntity.from(prisonNumber, createRandomCurrentReligion())
    assertThrows(DataIntegrityViolationException::class.java) {
      prisonReligionRepository.saveAndFlush(religion)
      prisonReligionRepository.saveAndFlush(religion2)
    }
  }

  private fun createRandomCurrentReligion() = PrisonReligion(
    nomisReligionId = randomDigit(),
    changeReasonKnown = randomBoolean(),
    comments = randomName(),
    verified = randomBoolean(),
    religionCode = randomReligionCode(),
    startDate = randomDate(),
    endDate = randomDate(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    current = true,
  )
}
