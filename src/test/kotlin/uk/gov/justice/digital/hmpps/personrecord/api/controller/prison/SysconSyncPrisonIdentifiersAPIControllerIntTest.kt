package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonIdentifiersAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class HappyPath {

    @Test
    fun `should respond with 501 as not currently implemented`() {
      webTestClient.put()
        .uri(identifiersUrl(randomPrisonNumber()))
        .bodyValue(identifiersRequest())
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isEqualTo(501)
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `should return bad request when body is missing`() {
      webTestClient.put()
        .uri(identifiersUrl(randomPrisonNumber()))
        .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isBadRequest
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      webTestClient.put()
        .uri(identifiersUrl(randomPrisonNumber()))
        .bodyValue(identifiersRequest())
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Forbidden: Access Denied")
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.put()
        .uri(identifiersUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun identifiersRequest() = PrisonIdentifiersRequest(
    identifiers = listOf(
      PrisonIdentifier(
        nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 1),
        type = IdentifierType.PNC,
        value = "2000/1234567A",
        comment = "comment",
        issuedDate = LocalDate.of(2020, 1, 1),
        verified = true,
        createDateTime = LocalDateTime.of(2021, 1, 1, 12, 0),
        createUserId = "createUserId",
      ),
    ),
  )

  private fun identifiersUrl(prisonNumber: String, cprAliasId: String = UUID.randomUUID().toString()) = "/syscon-sync/person/$prisonNumber/alias/$cprAliasId/identifiers"
}
