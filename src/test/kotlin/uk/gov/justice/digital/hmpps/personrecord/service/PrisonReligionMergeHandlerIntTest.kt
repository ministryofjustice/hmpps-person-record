package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.HISTORIC
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.ADV
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.DRU
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.EODX
import java.time.LocalDate
import java.time.LocalDateTime

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
        prisonReligionEntity {
          it.prisonNumber = fromPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 1, 25)
          it.endDate = LocalDate.of(2021, 4, 12)
          it.code = AGNO
        },
        prisonReligionEntity {
          it.prisonNumber = fromPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 4, 12)
          it.code = BAHA
          it.prisonRecordType = CURRENT
        },
      )
      val prisonerToreligionHistory = listOf(
        prisonReligionEntity {
          it.prisonNumber = toPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 1, 1)
          it.endDate = LocalDate.of(2021, 4, 24)
          it.code = CALV
        },
        prisonReligionEntity {
          it.prisonNumber = toPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 4, 10)
          it.code = DRU
          it.prisonRecordType = CURRENT
        },
      )
      prisonReligionRepository.saveAll(prisonerFromReligionHistory + prisonerToreligionHistory)

      // Method under test
      prisonReligionMergeHandler.handleMerge(fromPrisoner, toPrisoner)

      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(fromPrisoner.prisonNumber!!)).isEmpty()
      val prisonerFromReligionHistoryMerged = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisoner.prisonNumber!!)
      assertThat(prisonerFromReligionHistoryMerged).hasSize((prisonerFromReligionHistory + prisonerToreligionHistory).size)
      // Only one current religion and it was the one that had the latest start date
      val currentReligions = prisonerFromReligionHistoryMerged.filter { it.prisonRecordType == CURRENT }
      assertThat(currentReligions).hasSize(1)
      val currentReligion = currentReligions.single()
      assertThat(currentReligion.code).isEqualTo(BAHA)
      // The religion that is no longer current should have an end date set to the start date of the one which is current
      assertThat(prisonerFromReligionHistoryMerged.first { it.code == DRU }.endDate).isEqualTo(currentReligion.startDate)
      // Check that the to person has the correct religion
      assertThat(personRepository.findByPrisonNumber(toPrisoner.prisonNumber!!)?.religion).isEqualTo(currentReligion.code)
    }

    fun `should merge religion history when only one prisoner has a history`() {
      val fromPrisoner = createPerson(createRandomPrisonPersonDetails())
      val toPrisoner = createPerson(createRandomPrisonPersonDetails())
      val prisonerFromReligionHistory =
        prisonReligionEntity {
          it.prisonNumber = fromPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 1, 25)
          it.endDate = LocalDate.of(2021, 4, 12)
          it.code = EODX
        }
      prisonReligionRepository.save(prisonerFromReligionHistory)

      // Method under test
      prisonReligionMergeHandler.handleMerge(fromPrisoner, toPrisoner)

      // The religion should have been moved to the to person
      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(fromPrisoner.prisonNumber!!)).isEmpty()
      val prisonerFromReligionHistoryMerged = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisoner.prisonNumber!!)
      assertThat(prisonerFromReligionHistoryMerged).hasSize(1)
      assertThat(prisonerFromReligionHistoryMerged.single().code).isEqualTo(EODX)
      assertThat(prisonerFromReligionHistoryMerged.single().prisonRecordType).isEqualTo(CURRENT)
      assertThat(personRepository.findByPrisonNumber(toPrisoner.prisonNumber!!)?.religion).isEqualTo(EODX)
    }

    private fun prisonReligionEntity(config: (PrisonReligionEntity) -> Unit): PrisonReligionEntity {
      val entity = PrisonReligionEntity(
        prisonRecordType = HISTORIC,
        prisonNumber = "A1234BC",
        code = ADV,
        changeReasonKnown = false,
        startDate = LocalDate.now(),
        createDateTime = LocalDateTime.now(),
        createUserId = "abcdefg",
      )
      config(entity)
      return entity
    }
  }
}
