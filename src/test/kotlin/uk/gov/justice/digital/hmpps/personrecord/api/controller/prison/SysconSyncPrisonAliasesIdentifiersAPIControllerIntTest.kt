package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate

class SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class Validation {

    @Test
    fun `should respond with 501 as not currently implemented`() {
      webTestClient.post()
        .uri(aliasesIdentifiersUrl(randomPrisonNumber()))
        .bodyValue(PrisonAliasesAndIdentifiersRequest(aliases = emptyList(), identifiers = emptyList()))
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
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(aliasesIdentifiersUrl(randomPrisonNumber()))
        .bodyValue(
          PrisonAliasesAndIdentifiersRequest(
            aliases = listOf(
              PrisonAlias(
                nomisOffenderId = 10000L,
                titleCode = TitleCode.MR,
                firstName = "firstName",
                middleNames = "middleName",
                lastName = "lastName",
                dateOfBirth = LocalDate.of(1990, 1, 1),
                sexCode = SexCode.M,
                isPrimary = true,
              ),
            ),
            identifiers = listOf(
              PrisonIdentifier(
                nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 0),
                type = IdentifierType.PNC,
                value = "2000/1234567A",
                comment = "comment",
              ),
            ),
          ),
        )
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
        .uri(aliasesIdentifiersUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  private fun aliasesIdentifiersUrl(prisonNumber: String) = "/syscon-sync/aliases-identifiers/$prisonNumber"
}
