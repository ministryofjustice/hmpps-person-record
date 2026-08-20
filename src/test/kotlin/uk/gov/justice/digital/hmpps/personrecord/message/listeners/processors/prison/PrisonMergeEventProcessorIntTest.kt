package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonMergedInfo
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
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

class PrisonMergeEventProcessorIntTest : MessagingMultiNodeTestBase() {

  @Autowired
  lateinit var prisonMergeEventProcessor: PrisonMergeEventProcessor

  @Autowired
  lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  @ActiveProfiles("prod")
  inner class ShouldNotMergeReligionsInProd {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should not merge the religions while prod profile is set`() {
      val toPrisonerNumber = randomPrisonNumber()
      val fromPrisonerNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(Person(prisonNumber = toPrisonerNumber, sourceSystem = NOMIS))
        .addPerson(
          Person(prisonNumber = fromPrisonerNumber, sourceSystem = NOMIS),
        )
      prisonReligionRepository.save(
        prisonReligionEntity(
          prisonNumber = toPrisonerNumber,
          startDate = LocalDate.of(2021, 1, 1),
          code = CALV,
        ),
      )
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonerNumber))

      // Method under test
      prisonMergeEventProcessor.processEvent(fromPrisonNumber = fromPrisonerNumber, toPrisonNumber = toPrisonerNumber)

      assertThat(personRepository.findByPrisonNumber(toPrisonerNumber)?.religion).isNull()
    }
  }

  @Nested
  inner class MergingReligion {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should merge the religions to the to person`() {
      val toPrisonerNumber = randomPrisonNumber()
      val fromPrisonerNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(Person(prisonNumber = toPrisonerNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = fromPrisonerNumber, sourceSystem = NOMIS))
      prisonReligionRepository.saveAll(
        listOf(
          prisonReligionEntity(
            prisonNumber = toPrisonerNumber,
            startDate = LocalDate.of(2021, 1, 1),
            code = CALV,
          ),
          prisonReligionEntity(
            prisonNumber = fromPrisonerNumber,
            startDate = LocalDate.of(2021, 1, 25),
            code = AGNO,
          ),
        ),
      )
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonerNumber))

      // Method under test
      prisonMergeEventProcessor.processEvent(fromPrisonNumber = fromPrisonerNumber, toPrisonNumber = toPrisonerNumber)

      assertThat(personRepository.findByPrisonNumber(toPrisonerNumber)?.religion).isEqualTo(AGNO)
      assertThat(prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(toPrisonerNumber))
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

  companion object {
    fun prisonPersonMerged(toPrisonNumber: String, fromPrisonNumber: String) = PrisonPersonMerged(
      personReference = PersonReference(listOf(PersonIdentifier("NOMS", toPrisonNumber))),
      additionalInformation = PrisonPersonMergedInfo(sourcePrisonNumber = fromPrisonNumber),
    )
  }
}
