package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAliasesAPIControllerIntTest(
  @Autowired private val pseudonymRepository: PseudonymRepository,
  @Autowired private val prisonPseudonymRepository: PrisonPseudonymRepository,
) : WebTestBase() {

  @Nested
  inner class GetAlias {

    @Nested
    inner class SuccessfulProcessing {

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
    inner class Auth {

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
    inner class SuccessfulProcessing {
      @Test
      fun `should respond with an empty response`() {
        val responseBody = sendPostRequestAsserted<SysconAliasMapping>(
          url = aliasesUrl(randomPrisonNumber()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = HttpStatus.CREATED,
          body = PrisonAlias(
            nomisOffenderId = 10000L,
            isPrimary = true,
            createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
            createUserId = "createUserId",
          ),
        ).returnResult().responseBody!!
        assertThat(responseBody).isEqualTo(SysconAliasMapping())
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
    inner class Auth {

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
    inner class SuccessfulProcessing {

      @Test
      fun `should update the alias fields only, leaving name type, create fields and identifiers untouched`() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val alias = personEntity.getAliases().single()
        val primaryName = personEntity.getPrimaryName()
        val request = prisonAlias().copy(
          isPrimary = true,
          createDateTime = LocalDateTime.of(2001, 1, 1, 1, 0),
          createUserId = "newCreateUserId",
          modifyDateTime = LocalDateTime.of(2024, 2, 2, 2, 0),
          modifyUserId = "modifyUserId",
        )

        webTestClient.put()
          .uri(aliasUrl(prisonNumber, alias.updateId.toString()))
          .bodyValue(request)
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isNoContent

        val updated = pseudonymRepository.findByUpdateId(alias.updateId!!)!!
        assertThat(updated.nameType).isEqualTo(ALIAS)
        assertThat(updated.titleCode).isEqualTo(request.titleCode)
        assertThat(updated.firstName).isEqualTo(request.firstName)
        assertThat(updated.middleNames).isEqualTo(request.middleNames)
        assertThat(updated.lastName).isEqualTo(request.lastName)
        assertThat(updated.dateOfBirth).isEqualTo(request.dateOfBirth)
        assertThat(updated.sexCode).isEqualTo(request.sexCode)
        assertThat(updated.ethnicityCode).isEqualTo(request.ethnicity)
        assertThat(updated.birthplace).isEqualTo(request.birthPlace)
        assertThat(updated.birthCountryCode).isEqualTo(request.birthCountry)
        assertThat(updated.modifyDateTime).isEqualTo(request.modifyDateTime)
        assertThat(updated.modifyUserId).isEqualTo(request.modifyUserId)
        assertThat(updated.createDateTime).isEqualTo(alias.createDateTime)
        assertThat(updated.createUserId).isEqualTo(alias.createUserId)

        val prisonPseudonym = prisonPseudonymRepository.findByPseudonym(updated)
        assertThat(prisonPseudonym.suffix).isEqualTo(request.suffix)
        assertThat(prisonPseudonym.nameType).isEqualTo(request.nameType)

        val updatedPerson = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(updatedPerson.getPrimaryName().updateId).isEqualTo(primaryName.updateId)
        assertThat(updatedPerson.ethnicityCode).isEqualTo(personEntity.ethnicityCode)
        assertThat(updatedPerson.references.filter { it.pseudonym?.id == alias.id }).isEmpty()
      }

      @Test
      fun `should update the person ethnicity when updating the primary name`() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val primaryName = personEntity.getPrimaryName()
        val request = prisonAlias().copy(isPrimary = false, ethnicity = EthnicityCode.W9)

        webTestClient.put()
          .uri(aliasUrl(prisonNumber, primaryName.updateId.toString()))
          .bodyValue(request)
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isNoContent

        val updatedPerson = personRepository.findByPrisonNumber(prisonNumber)!!
        assertThat(updatedPerson.getPrimaryName().updateId).isEqualTo(primaryName.updateId)
        assertThat(updatedPerson.getPrimaryName().lastName).isEqualTo(request.lastName)
        assertThat(updatedPerson.ethnicityCode).isEqualTo(EthnicityCode.W9)
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

      @Test
      fun `should return not found when the alias does not exist`() {
        val prisonNumber = randomPrisonNumber()
        val cprAliasId = randomCprAliasId()

        webTestClient.put()
          .uri(aliasUrl(prisonNumber, cprAliasId))
          .bodyValue(prisonAlias())
          .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("userMessage")
          .isEqualTo("Not found: Alias with $cprAliasId not found for person with $prisonNumber")
      }
    }

    @Nested
    inner class Auth {

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
    inner class SuccessfulProcessing {

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
    inner class Auth {

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
