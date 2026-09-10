package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import java.util.UUID

@ActiveProfiles("preprod")
class SysconReligionControllerPreProdTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Test
  fun `should overwrite aliases upon an saving religions`() {
    val prisonNumber = randomPrisonNumber()
    val religionsInsertRequest = createRandomReligions()
    val originalPersons = createPerson(createRandomPrisonPersonDetails(prisonNumber))

    val actualResponseBody = postReligions(prisonNumber, religionsInsertRequest)
    assertCorrectValuesSaved(prisonNumber, religionsInsertRequest, actualResponseBody, originalPersons)
  }

  private fun postReligions(prisonNumber: String, religionsInsertRequest: List<PrisonReligionHistory>): SysconReligionResponseBody = sendPostRequestAsserted<SysconReligionResponseBody>(
    url = religionUrl(prisonNumber),
    body = PrisonReligionRequest(religionsInsertRequest),
    roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
    expectedStatus = HttpStatus.CREATED,
  ).returnResult().responseBody!!

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    requestBody: List<PrisonReligionHistory>,
    actualResponseBody: SysconReligionResponseBody,
    originalPersons: PersonEntity,
  ) {
    val actualReligionEntities = awaitNotNull { prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber) }
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!

    val expectedCurrReligion = requestBody.first { it.current }
    assertThat(personEntity.religion).isEqualTo(expectedCurrReligion.religionCode)
    assertThat(actualReligionEntities.size).isEqualTo(requestBody.size)
    assertThat(personEntity.getPrimaryName().updateId).isNotEqualTo(originalPersons.getPrimaryName().updateId)

    actualResponseBody.religionMappings.forEach { res ->
      val storedReligion = prisonReligionRepository.findByUpdateId(UUID.fromString(res.cprReligionId))!!
      val sentReligion = requestBody.find { it.nomisReligionId == res.nomisReligionId }!!
      assertThat(storedReligion.prisonNumber).isEqualTo(prisonNumber)
      assertThat(storedReligion.comments).isEqualTo(sentReligion.comments)
      assertThat(storedReligion.changeReasonKnown).isEqualTo(sentReligion.changeReasonKnown)
      assertThat(storedReligion.code).isEqualTo(sentReligion.religionCode)
      assertThat(storedReligion.startDate).isEqualTo(sentReligion.startDate)
      assertThat(storedReligion.endDate).isEqualTo(sentReligion.endDate)
      assertThat(storedReligion.modifyDateTime).isEqualTo(sentReligion.modifyDateTime)
      assertThat(storedReligion.modifyUserId).isEqualTo(sentReligion.modifyUserId)
      assertThat(storedReligion.prisonRecordType).isEqualTo(PrisonRecordType.from(sentReligion.current))
      assertThat(storedReligion.createDateTime).isEqualTo(sentReligion.createDateTime)
      assertThat(storedReligion.createUserId).isEqualTo(sentReligion.createUserId)
    }
  }

  private fun religionUrl(prisonNumber: String) = "/syscon-sync/religion/$prisonNumber"

  private fun createRandomReligions(): List<PrisonReligionHistory> = List((4..20).random()) { index ->
    if (index == 0) createPrisonReligionHistory(randomReligionCode(), true) else createPrisonReligionHistory(randomReligionCode(), false)
  }
}
