package uk.gov.justice.digital.hmpps.personrecord.repository

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonReligionSchemaTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Test
  fun `should fail to insert multiple current religions`() {
    val prisonNumber = randomPrisonNumber()
    val religion = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
    val religion2 = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
    assertThrows(DataIntegrityViolationException::class.java) {
      prisonReligionRepository.saveAndFlush(religion)
      prisonReligionRepository.saveAndFlush(religion2)
    }
  }
}
