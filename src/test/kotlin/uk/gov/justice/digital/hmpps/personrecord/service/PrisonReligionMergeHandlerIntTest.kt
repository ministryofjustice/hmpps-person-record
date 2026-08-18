package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.ADV
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.BAHA
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.DRU
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.EODX
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
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
      val fromPrisonerNumber = randomPrisonNumber()
      val toPrisonerNumber = randomPrisonNumber()
      val personKey = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(fromPrisonerNumber))
        .addPerson(createRandomPrisonPersonDetails(toPrisonerNumber))

      val prisonerFromReligionHistory = listOf(
        prisonReligionEntity(
          prisonNumber = fromPrisonerNumber,
          startDate = LocalDate.of(2021, 1, 25),
          endDate = LocalDate.of(2021, 4, 12),
          code = ADV,
        ),
        prisonReligionEntity(
          prisonNumber = fromPrisonerNumber,
          startDate = LocalDate.of(2021, 4, 12),
          code = BAHA,
        ),
      )
      val prisonerToReligionHistory = listOf(
        prisonReligionEntity(
          toPrisonerNumber,
          startDate = LocalDate.of(2021, 1, 1),
          endDate = LocalDate.of(2021, 4, 24),
          CALV,
        ),
        prisonReligionEntity(
          toPrisonerNumber,
          startDate = LocalDate.of(2021, 4, 10),
          code = DRU,
        ),
      )
      prisonReligionRepository.saveAll(prisonerFromReligionHistory + prisonerToReligionHistory)

      // Method under test
      prisonReligionMergeHandler.handleMerge(personKey.getPrisoner(fromPrisonerNumber), personKey.getPrisoner(toPrisonerNumber))

      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(fromPrisonerNumber)).isEmpty()
      val prisonerFromReligionHistoryMerged =
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisonerNumber)
      assertThat(prisonerFromReligionHistoryMerged).hasSize((prisonerFromReligionHistory + prisonerToReligionHistory).size)
      // Only one current religion and it was the one that had the latest start date
      val currentReligions = prisonerFromReligionHistoryMerged.filter { it.prisonRecordType == CURRENT }
      assertThat(currentReligions).hasSize(1)
      val currentReligion = currentReligions.single()
      assertThat(currentReligion.code).isEqualTo(BAHA)
      // The religion that is no longer current should have an end date set to now
      assertThat(prisonerFromReligionHistoryMerged.first { it.code == DRU }.endDate).isEqualTo(LocalDate.now())
      // Check that the to person has the correct religion
      assertThat(personRepository.findByPrisonNumber(toPrisonerNumber)?.religion).isEqualTo(currentReligion.code)
    }

    @Test
    fun `should use create date time when current religions have the same start date`() {
      val fromPrisonerNumber = randomPrisonNumber()
      val toPrisonerNumber = randomPrisonNumber()
      val personKey = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(fromPrisonerNumber))
        .addPerson(createRandomPrisonPersonDetails(toPrisonerNumber))

      val olderReligion = prisonReligionEntity(
        prisonNumber = fromPrisonerNumber,
        startDate = LocalDate.of(2021, 4, 12),
        code = ADV,
        createDateTime = LocalDateTime.of(2021, 4, 12, 10, 0),
      )
      val newerReligion = prisonReligionEntity(
        prisonNumber = toPrisonerNumber,
        startDate = LocalDate.of(2021, 4, 12),
        code = BAHA,
        createDateTime = LocalDateTime.of(2021, 4, 12, 11, 0),
      )
      prisonReligionRepository.saveAll(listOf(olderReligion, newerReligion))

      prisonReligionMergeHandler.handleMerge(personKey.getPrisoner(fromPrisonerNumber), personKey.getPrisoner(toPrisonerNumber))

      val currentReligion =
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisonerNumber)
          .single { it.prisonRecordType == CURRENT }
      assertThat(currentReligion.code).isEqualTo(BAHA)
      assertThat(personRepository.findByPrisonNumber(toPrisonerNumber)?.religion).isEqualTo(BAHA)
    }

    @Test
    fun `should leave only one null end date when multiple religions are current before merge`() {
      val fromPrisonerNumber = randomPrisonNumber()
      val toPrisonerNumber = randomPrisonNumber()
      val personKey = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(fromPrisonerNumber))
        .addPerson(createRandomPrisonPersonDetails(toPrisonerNumber))

      val fromPrisonerReligionHistory = listOf(
        prisonReligionEntity(
          prisonNumber = fromPrisonerNumber,
          startDate = LocalDate.of(2021, 4, 12),
          code = BAHA,
          prisonRecordType = PrisonRecordType.HISTORIC,
        ),
        prisonReligionEntity(
          prisonNumber = fromPrisonerNumber,
          startDate = LocalDate.of(2021, 4, 11),
          code = DRU,
        ),
      )
      val toPrisonerReligionHistory = listOf(
        prisonReligionEntity(
          prisonNumber = toPrisonerNumber,
          startDate = LocalDate.of(2021, 4, 10),
          code = ADV,
        ),
      )

      prisonReligionRepository.saveAll(fromPrisonerReligionHistory + toPrisonerReligionHistory)

      prisonReligionMergeHandler.handleMerge(personKey.getPrisoner(fromPrisonerNumber), personKey.getPrisoner(toPrisonerNumber))

      val mergedHistory = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisonerNumber)
      assertThat(mergedHistory.filter { it.endDate == null && it.prisonRecordType == CURRENT }).hasSize(1)
      assertThat(mergedHistory.filter { it.endDate == LocalDate.now() }).hasSize(2)
    }

    @Test
    fun `should merge religion history when only one prisoner has a history`() {
      val fromPrisonerNumber = randomPrisonNumber()
      val toPrisonerNumber = randomPrisonNumber()
      val personKey = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(fromPrisonerNumber))
        .addPerson(createRandomPrisonPersonDetails(toPrisonerNumber))
      val prisonerFromReligionHistory =
        prisonReligionEntity(
          prisonNumber = fromPrisonerNumber,
          startDate = LocalDate.of(2021, 1, 25),
          code = EODX,
        )
      prisonReligionRepository.save(prisonerFromReligionHistory)

      // Method under test
      prisonReligionMergeHandler.handleMerge(personKey.getPrisoner(fromPrisonerNumber), personKey.getPrisoner(toPrisonerNumber))

      // The religion should have been moved to the to person
      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(fromPrisonerNumber)).isEmpty()
      val prisonerFromReligionHistoryMerged =
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisonerNumber)
      assertThat(prisonerFromReligionHistoryMerged).hasSize(1)
      assertThat(prisonerFromReligionHistoryMerged.single().code).isEqualTo(EODX)
      assertThat(prisonerFromReligionHistoryMerged.single().prisonRecordType).isEqualTo(CURRENT)
      assertThat(personRepository.findByPrisonNumber(toPrisonerNumber)?.religion).isEqualTo(EODX)
    }
  }

  private fun PersonKeyEntity.getPrisoner(prisonNumber: String) = personEntities.first { it.prisonNumber == prisonNumber }
}
