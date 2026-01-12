package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion

class SysconReligionControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class Creation {

    @Test
    fun `should save religions against a new prison number`() {
      val prisonNumber = randomPrisonNumber()
      val religions = createRandomReligions()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postReligions(prisonNumber, religions)
      assertCorrectValuesSaved(prisonNumber, religions)
    }

    @Test
    fun `should save religions against a new prison number when an entry has a null code`() {
      val prisonNumber = randomPrisonNumber()
      val religions = createRandomReligions() + createRandomReligion(null, false)
      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postReligions(prisonNumber, religions)
      assertCorrectValuesSaved(prisonNumber, religions)
    }
  }

  @Nested
  inner class Update {

    @Test
    fun `should replace the existing religions against a prison number`() {
      val prisonNumber = randomPrisonNumber()
      val religions = createRandomReligions()

      createPerson(createRandomPrisonPersonDetails(prisonNumber))

      postReligions(prisonNumber, religions)
      assertCorrectValuesSaved(prisonNumber, religions)

      val updatedReligions = createRandomReligions()

      postReligions(prisonNumber, updatedReligions)
      assertCorrectValuesSaved(prisonNumber, updatedReligions)
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `should respond with bad request when no religions are posted`() {
      webTestClient.post()
        .uri(religionUrl(randomPrisonNumber()))
        .bodyValue(PrisonReligionRequest(emptyList()))
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `should return a 404 when the person does not exist`() {
      val prisonNumber = randomPrisonNumber()
      val expectedErrorMessage = "Not found: $prisonNumber"
      val religions = createRandomReligions() + createRandomReligion(null, false)

      webTestClient.post()
        .uri(religionUrl(prisonNumber))
        .bodyValue(PrisonReligionRequest(religions))
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return a 400 when more than one current religion is sent`() {
      val prisonNumber = randomPrisonNumber()
      val religions = listOf(createRandomReligion(randomReligion(), true), createRandomReligion(randomReligion(), true))
      createPerson(createRandomPrisonPersonDetails(prisonNumber))
      val reqBody = PrisonReligionRequest(religions)

      webTestClient.post()
        .uri(religionUrl(prisonNumber))
        .bodyValue(reqBody)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Bad request: More than one current religion was sent for $reqBody")
    }

    @Test
    fun `should return a 400 when no current religion is sent`() {
      val prisonNumber = randomPrisonNumber()
      val religions = listOf(createRandomReligion(randomReligion(), false), createRandomReligion(randomReligion(), false))
      createPerson(createRandomPrisonPersonDetails(prisonNumber))
      val reqBody = PrisonReligionRequest(religions)

      webTestClient.post()
        .uri(religionUrl(prisonNumber))
        .bodyValue(reqBody)
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Bad request: No current religion was sent for $reqBody")
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(religionUrl(randomPrisonNumber()))
        .bodyValue(PrisonReligionRequest(createRandomReligions()))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.post()
        .uri(religionUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun postReligions(prisonNumber: String, religions: List<PrisonReligion>) {
    webTestClient
      .post()
      .uri(religionUrl(prisonNumber))
      .bodyValue(PrisonReligionRequest(religions))
      .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun createRandomReligions(): List<PrisonReligion> = List((4..20).random()) { index ->
    if (index == 0) createRandomReligion(randomName(), true) else createRandomReligion(randomName(), false)
  }

  private fun createRandomReligion(code: String?, current: Boolean) = PrisonReligion(
    changeReasonKnown = randomBoolean(),
    comments = randomName(),
    verified = randomBoolean(),
    religionCode = code,
    startDate = randomDate(),
    endDate = randomDate(),
    modifyDateTime = randomDateTime(),
    modifyUserId = randomName(),
    current = current,
  )

  private fun assertCorrectValuesSaved(
    prisonNumber: String,
    religions: List<PrisonReligion>,
  ) {
    val religionEntities = awaitNotNull { prisonReligionRepository.findByPrisonNumber(prisonNumber) }
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
    val expectedCurrReligion = religions.find { it.current }
    assertThat(expectedCurrReligion!!.religionCode).isEqualTo(personEntity.religion)

    assertThat(religionEntities.size).isEqualTo(religions.size)
    religions.zip(religionEntities).forEachIndexed { _, (sentReligion, storedReligion) ->
      assertThat(storedReligion.prisonNumber).isEqualTo(prisonNumber)
      assertThat(storedReligion.verified).isEqualTo(sentReligion.verified)
      assertThat(storedReligion.comments).isEqualTo(sentReligion.comments)
      assertThat(storedReligion.changeReasonKnown).isEqualTo(sentReligion.changeReasonKnown)
      assertThat(storedReligion.code).isEqualTo(sentReligion.religionCode)
      assertThat(storedReligion.startDate).isEqualTo(sentReligion.startDate)
      assertThat(storedReligion.endDate).isEqualTo(sentReligion.endDate)
      assertThat(storedReligion.modifyDateTime).isEqualTo(sentReligion.modifyDateTime)
      assertThat(storedReligion.modifyUserId).isEqualTo(sentReligion.modifyUserId)
      assertThat(storedReligion.prisonRecordType).isEqualTo(PrisonRecordType.from(sentReligion.current))
    }
  }

  private fun religionUrl(prisonNumber: String) = "/syscon-sync/religion/$prisonNumber"
}
