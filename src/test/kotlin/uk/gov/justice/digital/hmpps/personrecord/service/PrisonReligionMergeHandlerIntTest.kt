package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.ADV
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.DRU
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.EODX
import java.time.LocalDate

class PrisonReligionMergeHandlerIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var prisonReligionMergeHandler: PrisonReligionMergeHandler

  @Autowired
  lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class MergeReligion {

    @Test
    fun `should merge religion history`() {
      val fromPrisoner = createPerson(createRandomPrisonPersonDetails())
      val toPrisoner = createPerson(createRandomPrisonPersonDetails())

      val prisonerFromReligionHistory = listOf(
        prisonReligionEntity(
          prisonNumber = fromPrisoner.prisonNumber!!,
          startDate = LocalDate.of(2021, 1, 25),
          endDate = LocalDate.of(2021, 4, 12),
          code = ADV,
        ),
        prisonReligionEntity(
          prisonNumber = fromPrisoner.prisonNumber!!,
          startDate = LocalDate.of(2021, 4, 12),
          code = BAHA,
        ),
      )
      val prisonerToReligionHistory = listOf(
        prisonReligionEntity(
          toPrisoner.prisonNumber!!,
          startDate = LocalDate.of(2021, 1, 1),
          endDate = LocalDate.of(2021, 4, 24),
          CALV,
        ),
        prisonReligionEntity(
          toPrisoner.prisonNumber!!,
          startDate = LocalDate.of(2021, 4, 10),
          code = DRU,
        ),
      )
      prisonReligionRepository.saveAll(prisonerFromReligionHistory + prisonerToReligionHistory)

      // Method under test
      prisonReligionMergeHandler.handleMerge(fromPrisoner, toPrisoner)

      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(fromPrisoner.prisonNumber!!)).isEmpty()
      val prisonerFromReligionHistoryMerged =
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisoner.prisonNumber!!)
      assertThat(prisonerFromReligionHistoryMerged).hasSize((prisonerFromReligionHistory + prisonerToReligionHistory).size)
      // Only one current religion and it was the one that had the latest start date
      val currentReligions = prisonerFromReligionHistoryMerged.filter { it.prisonRecordType == CURRENT }
      assertThat(currentReligions).hasSize(1)
      val currentReligion = currentReligions.single()
      assertThat(currentReligion.code).isEqualTo(BAHA)
      // The religion that is no longer current should have an end date set to now
      assertThat(prisonerFromReligionHistoryMerged.first { it.code == DRU }.endDate).isEqualTo(LocalDate.now())
      // Check that the to person has the correct religion
      assertThat(personRepository.findByPrisonNumber(toPrisoner.prisonNumber!!)?.religion).isEqualTo(currentReligion.code)
    }

    fun `should merge religion history when only one prisoner has a history`() {
      val fromPrisoner = createPerson(createRandomPrisonPersonDetails())
      val toPrisoner = createPerson(createRandomPrisonPersonDetails())
      val prisonerFromReligionHistory =
        prisonReligionEntity(
          prisonNumber = fromPrisoner.prisonNumber!!,
          startDate = LocalDate.of(2021, 1, 25),
          endDate = LocalDate.of(2021, 4, 12),
          code = EODX,
        )
      prisonReligionRepository.save(prisonerFromReligionHistory)

      // Method under test
      prisonReligionMergeHandler.handleMerge(fromPrisoner, toPrisoner)

      // The religion should have been moved to the to person
      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(fromPrisoner.prisonNumber!!)).isEmpty()
      val prisonerFromReligionHistoryMerged =
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisoner.prisonNumber!!)
      assertThat(prisonerFromReligionHistoryMerged).hasSize(1)
      assertThat(prisonerFromReligionHistoryMerged.single().code).isEqualTo(EODX)
      assertThat(prisonerFromReligionHistoryMerged.single().prisonRecordType).isEqualTo(CURRENT)
      assertThat(personRepository.findByPrisonNumber(toPrisoner.prisonNumber!!)?.religion).isEqualTo(EODX)
    }
  }
}
