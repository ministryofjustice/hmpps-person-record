package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
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
      val fromPrisoner = createRandomPrisonPersonDetails()
      val toPrisoner = createRandomPrisonPersonDetails()

      val prisonerFromReligiousHistory = listOf(
        prisonReligionEntity {
          it.prisonNumber = fromPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 1, 25)
          it.endDate = LocalDate.of(2021, 4, 12)
          it.code = ReligionCode.AGNO
        },
        prisonReligionEntity {
          it.prisonNumber = fromPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 4, 12)
          it.code = ReligionCode.BAHA
          it.prisonRecordType = PrisonRecordType.CURRENT
        }
      )
      val prisonerToReligiousHistory = listOf(
        prisonReligionEntity {
          it.prisonNumber = toPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 1, 1)
          it.endDate = LocalDate.of(2021, 4, 24)
          it.code = ReligionCode.CALV
        },
        prisonReligionEntity {
          it.prisonNumber = toPrisoner.prisonNumber!!
          it.startDate = LocalDate.of(2021, 4, 10)
          it.code = ReligionCode.DRU
          it.prisonRecordType = PrisonRecordType.CURRENT
        }
      )
      prisonReligionRepository.saveAll(prisonerFromReligiousHistory + prisonerToReligiousHistory )
    }


    private fun prisonReligionEntity(config: (PrisonReligionEntity) -> Unit): PrisonReligionEntity {
      val entity = PrisonReligionEntity(
        prisonRecordType = PrisonRecordType.HISTORIC,
        prisonNumber = "",
        code = ReligionCode.ADV,
        changeReasonKnown = false,
        startDate = LocalDate.now(),
        createDateTime = LocalDateTime.now(),
        createUserId = "ABC"
      )
      config(entity)
      return entity
    }
  }
}
