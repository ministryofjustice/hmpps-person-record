package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import java.time.LocalDate

@ActiveProfiles("prod")
class PrisonMergeEventProcessorIntTestProd : MessagingTestBase() {

  @Autowired
  private lateinit var prisonMergeEventProcessor: PrisonMergeEventProcessor

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Test
  fun `should continue to update person aliases upon a merge`() {
    val toPrisonerNumber = randomPrisonNumber()
    val fromPrisonerNumber = randomPrisonNumber()
    createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonNumber = toPrisonerNumber))
      .addPerson(createRandomPrisonPersonDetails(prisonNumber = fromPrisonerNumber))
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

    val newFirstName = randomName()
    val newLastName = randomName()
    val newDateOfBirth = randomDate()
    stubPrisonResponse(
      ApiResponseSetup(
        prisonNumber = toPrisonerNumber,
        aliases = listOf(
          ApiResponseSetupAlias(
            firstName = newFirstName,
            lastName = newLastName,
            dateOfBirth = newDateOfBirth,
          ),
        ),
      ),
    )

    stubPersonMatchUpsert()
    stubDeletePersonMatch()
    prisonMergeEventProcessor.processEvent(fromPrisonNumber = fromPrisonerNumber, toPrisonNumber = toPrisonerNumber)

    val actualPersonEntity = personRepository.findByPrisonNumber(toPrisonerNumber)!!
    val actualToPersonsAlias = actualPersonEntity.getAliases().map { Alias.from(it) }.first()
    assertThat(actualToPersonsAlias.firstName).isEqualTo(newFirstName)
    assertThat(actualToPersonsAlias.lastName).isEqualTo(newLastName)
    assertThat(actualToPersonsAlias.dateOfBirth).isEqualTo(newDateOfBirth)
  }
}
