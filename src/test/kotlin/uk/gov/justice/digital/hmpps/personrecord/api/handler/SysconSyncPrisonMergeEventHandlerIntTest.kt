package uk.gov.justice.digital.hmpps.personrecord.api.handler

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.PrisonMergeEventHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import java.time.LocalDate

class SysconSyncPrisonMergeEventHandlerIntTest(
  @Autowired private val prisonMergeEventHandler: PrisonMergeEventHandler,
  @Autowired private val prisonReligionRepository: PrisonReligionRepository,
) : MessagingTestBase() {

  @Nested
  inner class MergeAddressesAndContacts {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should return mappings for the merge`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(Person(prisonNumber = toPrisonNumber, sourceSystem = SourceSystemType.NOMIS))
        .addPerson(Person(prisonNumber = fromPrisonNumber, sourceSystem = SourceSystemType.NOMIS))
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))
      val response = prisonMergeEventHandler.handleMerge(toPrisonNumber = toPrisonNumber, prisonMerge = validPrisonMerge(fromPrisonNumber))
      Assertions.assertThat(response.toPrisonNumber).isEqualTo(toPrisonNumber)
    }
  }

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
        .addPerson(Person(prisonNumber = toPrisonNumber, sourceSystem = SourceSystemType.NOMIS))
        .addPerson(Person(prisonNumber = fromPrisonNumber, sourceSystem = SourceSystemType.NOMIS))
      prisonReligionRepository.saveAll(
        listOf(
          prisonReligionEntity(
            prisonNumber = toPrisonNumber,
            startDate = LocalDate.of(2021, 1, 1),
            code = ReligionCode.CALV,
          ),
          prisonReligionEntity(
            prisonNumber = fromPrisonNumber,
            startDate = LocalDate.of(2021, 1, 25),
            code = ReligionCode.AGNO,
          ),
        ),
      )
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))

      // Method under test
      prisonMergeEventHandler.handleMerge(toPrisonNumber = toPrisonNumber, prisonMerge = validPrisonMerge(fromPrisonNumber))

      Assertions.assertThat(personRepository.findByPrisonNumber(toPrisonNumber)?.religion).isEqualTo(ReligionCode.AGNO)
      Assertions.assertThat(
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(
          toPrisonNumber,
        ),
      )
        .satisfiesExactly(
          {
            Assertions.assertThat(it.code).isEqualTo(ReligionCode.AGNO)
            Assertions.assertThat(it.prisonRecordType).isEqualTo(PrisonRecordType.CURRENT)
          },
          {
            Assertions.assertThat(it.code).isEqualTo(ReligionCode.CALV)
            Assertions.assertThat(it.prisonRecordType).isEqualTo(PrisonRecordType.HISTORIC)
          },
        )
    }
  }

  private fun validPrisonMerge(fromPrisonNumber: String) = PrisonMerge(
    fromPrisonNumber = fromPrisonNumber,
    toPrisonAddressesAndContactsRequest = SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.validPrisonAddressesAndContactsRequest,
  )
}
