package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.CURRENT
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType.HISTORIC
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import java.time.LocalDate

class PrisonMergeEventProcessorIntTest(
  @Autowired private val prisonMergeEventProcessor: PrisonMergeEventProcessor,
  @Autowired private val prisonReligionRepository: PrisonReligionRepository,
) : MessagingTestBase() {

  @Nested
  inner class MergingReligion {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should merge the religions to the to person`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(Person(prisonNumber = toPrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = fromPrisonNumber, sourceSystem = NOMIS))
      prisonReligionRepository.saveAll(
        listOf(
          prisonReligionEntity(
            prisonNumber = toPrisonNumber,
            startDate = LocalDate.of(2021, 1, 1),
            code = CALV,
          ),
          prisonReligionEntity(
            prisonNumber = fromPrisonNumber,
            startDate = LocalDate.of(2021, 1, 25),
            code = AGNO,
          ),
        ),
      )
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))

      // Method under test
      prisonMergeEventProcessor.processEvent(fromPrisonNumber = fromPrisonNumber, toPrisonNumber = toPrisonNumber)

      assertThat(personRepository.findByPrisonNumber(toPrisonNumber)?.religion).isEqualTo(AGNO)
      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisonNumber))
        .satisfiesExactly(
          {
            assertThat(it.code).isEqualTo(AGNO)
            assertThat(it.prisonRecordType).isEqualTo(CURRENT)
          },
          {
            assertThat(it.code).isEqualTo(CALV)
            assertThat(it.prisonRecordType).isEqualTo(HISTORIC)
          },
        )
    }
  }
}
