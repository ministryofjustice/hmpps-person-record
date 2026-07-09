package uk.gov.justice.digital.hmpps.personrecord.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PrisonReligionSchemaTest(
  @Autowired private val prisonReligionRepository: PrisonReligionRepository,
  @Autowired private val jdbcTemplate: JdbcTemplate,
) : IntegrationTestBase() {

  @Test
  fun `should fail to insert multiple current religions`() {
    val prisonNumber = randomPrisonNumber()
    val religion = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
    val religion2 = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
    prisonReligionRepository.saveAndFlush(religion)
    assertThrows(DataIntegrityViolationException::class.java) {
      prisonReligionRepository.saveAndFlush(religion2)
    }
  }

  @Test
  fun `should insert religion code not ordinal into the database`() {
    val prisonNumber = randomPrisonNumber()
    val religion = PrisonReligionEntity.from(prisonNumber, createRandomReligion(current = true))
    prisonReligionRepository.saveAndFlush(religion)
    val religionCode = jdbcTemplate.queryForObject<String>(
      "SELECT religion_code FROM prison_religion WHERE prison_number = ?",
      prisonNumber,
    )
    assertThat(religionCode).isEqualTo(religion.code.name)
  }
}
