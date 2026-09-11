package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionSaveResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

@ActiveProfiles("prod")
class SysconSyncPrisonReligionPostAPIControllerProdTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Test
  fun `should overwrite aliases upon an prison religion save`() {
    val prisonNumber = randomPrisonNumber()
    val originalPerson = createPerson(createRandomPrisonPersonDetails(prisonNumber))

    val requestBody = createPrisonReligionHistory()
    sendPostRequestAsserted<PrisonReligionSaveResponse>(
      url = prisonReligionPostEndpoint(prisonNumber),
      body = requestBody,
      roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
      expectedStatus = HttpStatus.CREATED,
    )

    awaitAssert {
      val personEntity = personRepository.findByPrisonNumber(prisonNumber) ?: fail("No person found with id $prisonNumber")
      assertThat(personEntity.religion).isEqualTo(requestBody.religionCode)
      assertThat(personEntity.getPrimaryName().updateId).isNotEqualTo(originalPerson.getPrimaryName().updateId)

      val actualPrisonReligionEntities = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)
      assertThat(actualPrisonReligionEntities).hasSize(1)
      assertPrisonReligionEntityColumns(prisonNumber, actualPrisonReligionEntities.first(), requestBody)
    }
  }

  private fun assertPrisonReligionEntityColumns(
    prisonNumber: String,
    actual: PrisonReligionEntity,
    expected: PrisonReligionHistory,
  ) {
    assertThat(actual.updateId.toString()).isNotEmpty()
    assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
    assertThat(actual.changeReasonKnown).isEqualTo(expected.changeReasonKnown)
    assertThat(actual.comments).isEqualTo(expected.comments)
    assertThat(actual.startDate).isEqualTo(expected.startDate)
    assertThat(actual.endDate).isEqualTo(expected.endDate)
    assertThat(actual.modifyDateTime).isEqualTo(expected.modifyDateTime)
    assertThat(actual.prisonRecordType).isEqualTo(PrisonRecordType.from(expected.current))
    assertThat(actual.createDateTime).isEqualTo(expected.createDateTime)
    assertThat(actual.createUserId).isEqualTo(expected.createUserId)
    assertThat(actual.modifyUserId).isEqualTo(expected.modifyUserId)
  }

  private fun prisonReligionPostEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/religion"
}
