package uk.gov.justice.digital.hmpps.personrecord.api.controller.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.ExactMatchSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.ExactMatchSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class ExactMatchSearchControllerE2ETest : E2ETestBase() {
  private val url = "/person/search/exact-match"

  @Nested
  inner class Success {

    @Test
    fun `should return person ids when multiple exact matches found`() {
      val probationMatch = createRandomProbationPersonDetails()
      val cluster = createPersonKey()
        .addPerson(probationMatch)
        .addPerson(probationMatch.copy(crn = null, prisonNumber = randomPrisonNumber(), sourceSystem = SourceSystemType.NOMIS))

      val prisonMatch = cluster.personEntities.first { it.prisonNumber != null }

      val actualResponse = sendPostRequestAsserted<ExactMatchSearchResponse>(
        url = url,
        body = ExactMatchSearchRequest(
          firstName = prisonMatch.getPrimaryName().firstName!!,
          lastName = prisonMatch.getPrimaryName().lastName!!,
          dateOfBirth = prisonMatch.getPrimaryName().dateOfBirth!!,
          pnc = prisonMatch.getPnc()!!,
        ),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      val expectedResponse = ExactMatchSearchResponse(
        crns = listOf(probationMatch.crn!!),
        prisonNumbers = listOf(prisonMatch.prisonNumber!!),
        defendantIds = emptyList(),
        cids = emptyList(),
      )
      assertThat(actualResponse).isEqualTo(expectedResponse)
    }

    @Test
    fun `should return person id when one exact match found`() {
      val cluster = createPersonKey()
        .addPerson(createRandomProbationPersonDetails())
        .addPerson(createRandomPrisonPersonDetails())

      val existingPerson = cluster.personEntities.first { it.prisonNumber != null }

      val actualResponse = sendPostRequestAsserted<ExactMatchSearchResponse>(
        url = url,
        body = ExactMatchSearchRequest(
          firstName = existingPerson.getPrimaryName().firstName!!,
          lastName = existingPerson.getPrimaryName().lastName!!,
          dateOfBirth = existingPerson.getPrimaryName().dateOfBirth!!,
          pnc = existingPerson.getPnc()!!,
        ),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.OK,
      ).returnResult().responseBody!!

      val expectedResponse = ExactMatchSearchResponse(
        crns = emptyList(),
        prisonNumbers = listOf(existingPerson.prisonNumber!!),
        defendantIds = emptyList(),
        cids = emptyList(),
      )
      assertThat(actualResponse).isEqualTo(expectedResponse)
    }

    @Test
    fun `should return not found when no exact match found`() {
      val cluster = createPersonKey()
        .addPerson(createRandomPrisonPersonDetails())

      val existingPerson = cluster.personEntities.first { it.prisonNumber != null }

      val actualResponse = sendPostRequestAsserted<ExactMatchSearchResponse>(
        url = url,
        body = ExactMatchSearchRequest(
          firstName = existingPerson.getPrimaryName().firstName!!,
          lastName = existingPerson.getPrimaryName().lastName!!,
          dateOfBirth = existingPerson.getPrimaryName().dateOfBirth!!,
          pnc = randomLongPnc(),
        ),
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.NOT_FOUND,
      ).returnResult().responseBody!!

      assertThat(actualResponse).isEqualTo(ExactMatchSearchResponse())
    }
  }

  @Nested
  inner class Validation {
    @Test
    fun `should return bad request when request is invalid`() {
      sendPostRequestAsserted<ExactMatchSearchResponse>(
        url = url,
        body = """(
          lastName = "",
          dateOfBirth = "2022-99-34",
          pnc = null,
        }""",
        roles = listOf(API_READ_ONLY),
        expectedStatus = HttpStatus.BAD_REQUEST,
      )
    }
  }

  @Nested
  inner class Auth {
    @Test
    fun `should return not authorised when no authorisation`() {
      sendPostRequestAsserted<Unit>(
        url = url,
        body = Any(),
        roles = emptyList(),
        sendAuthorised = false,
        expectedStatus = HttpStatus.UNAUTHORIZED,
      )
    }

    @Test
    fun `should return forbidden when wrong role supplied`() {
      sendPostRequestAsserted<Unit>(
        url = url,
        body = """{
          "firstName":"${randomName()}",
          "lastName":"${randomName()}",
          "dateOfBirth":"2022-02-04",
          "pnc":"${randomLongPnc()}"
        }""",
        roles = listOf("BAD_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }
}
