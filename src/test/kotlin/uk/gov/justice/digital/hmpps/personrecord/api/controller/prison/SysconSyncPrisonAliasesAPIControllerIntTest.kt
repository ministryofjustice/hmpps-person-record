package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAliasesAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class GetAlias {

    @Nested
    inner class HappyPath {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        webTestClient.get()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isEqualTo(501)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.get()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
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
        webTestClient.get()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class CreateAlias {

    @Nested
    inner class HappyPath {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        webTestClient.post()
          .uri(aliasesUrl(randomPrisonNumber()))
          .bodyValue(prisonAlias())
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
        webTestClient.post()
          .uri(aliasesUrl(randomPrisonNumber()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.post()
          .uri(aliasesUrl(randomPrisonNumber()))
          .bodyValue(prisonAlias())
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
        webTestClient.post()
          .uri(aliasesUrl(randomPrisonNumber()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class UpdateAlias {

    @Nested
    inner class HappyPath {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        webTestClient.put()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .bodyValue(prisonAlias())
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
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.put()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .bodyValue(prisonAlias())
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
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Nested
  inner class DeleteAlias {

    @Nested
    inner class HappyPath {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        webTestClient.delete()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isEqualTo(501)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        webTestClient.delete()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
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
        webTestClient.delete()
          .uri(aliasUrl(randomPrisonNumber(), randomCprAliasId()))
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  private fun prisonAlias() = PrisonAlias(
    nomisOffenderId = 10000L,
    titleCode = TitleCode.MR,
    firstName = "firstName",
    middleNames = "middleName",
    lastName = "lastName",
    dateOfBirth = LocalDate.of(1990, 1, 1),
    sexCode = SexCode.M,
    isPrimary = true,
    birthPlace = "London",
    birthCountry = CountryCode.UKR,
    ethnicity = EthnicityCode.A1,
    createUserId = "createUserId",
    suffix = PrisonPseudonymSuffix.I,
    nameType = PrisonAliasNameType.CN,
    createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
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

  private fun randomCprAliasId() = UUID.randomUUID().toString()

  private fun aliasesUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/alias"

  private fun aliasUrl(prisonNumber: String, cprAliasId: String) = "/syscon-sync/person/$prisonNumber/alias/$cprAliasId"
}
