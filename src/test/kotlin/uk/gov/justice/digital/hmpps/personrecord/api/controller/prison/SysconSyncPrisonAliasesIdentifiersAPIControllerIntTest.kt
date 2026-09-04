package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId as RequestNomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.NomisIdentifierId as ResponseNomisIdentifierId

class SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest : WebTestBase() {

  @Autowired
  lateinit var pseudonymRepository: PseudonymRepository

  private fun assertExpectedPseudonymMapping(
    actual: SysconAliasMapping,
    expectedCprPseudonymId: String,
    expectedNomisOffenderId: Long,
  ) = assertThat(actual).isEqualTo(
    SysconAliasMapping(
      nomisOffenderId = expectedNomisOffenderId,
      cprAliasId = expectedCprPseudonymId,
    ),
  )

  @Nested
  @ActiveProfiles("prod")
  inner class ProductionProfile {

    @Test
    fun `should have the correct profile active`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))
      postAndExpect(prisonNumber, validRequestBody())
        .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
    }
  }

  @Nested
  inner class Creation {

    @Test
    fun `successful save returns the correct response body`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))
      val requestBody = validRequestBody()

      val response = postAndExpect(prisonNumber, requestBody)
        .isCreated
        .expectBody<SysconAliasesAndIdentifiersResponseBody>()
        .returnResult()
        .responseBody!!

      val person = personRepository.findByPrisonNumber(prisonNumber)!!
      val references = person.references
      val pseudonyms = person.pseudonyms

      // Assert that the pseudonyms and references have been created correctly
      assertThat(pseudonyms).hasSize(2)
      assertPseudonymMatchesRequest(pseudonym = pseudonyms[0], request = requestBody.aliases[0])
      assertPseudonymMatchesRequest(pseudonym = pseudonyms[1], request = requestBody.aliases[1])
      assertThat(references).hasSize(2)
      assertReferenceMatchesRequest(referenceEntity = references[0], request = requestBody.identifiers[0])
      assertReferenceMatchesRequest(referenceEntity = references[1], request = requestBody.identifiers[1])

      // Assert that the top level person attributes have been updated from the primary pseudonym
      val primaryPseudonym = requestBody.aliases.first { it.isPrimary!! }
      assertThat(person.ethnicityCode).isEqualTo(primaryPseudonym.ethnicity)
      assertThat(person.birthplace).isEqualTo(primaryPseudonym.birthPlace)
      assertThat(person.birthCountryCode).isEqualTo(primaryPseudonym.birthCountry)

      // Assert that the mappings are as expected in the response body
      assertThat(response.prisonNumber).isEqualTo(prisonNumber)
      assertThat(response.identifiersMappings).hasSize(2)
      assertExpectedReferenceMapping(
        actual = response.identifiersMappings[0],
        expectedCprReferenceId = references[0].updateId.toString(),
        expectedNomisIdentifierId = requestBody.identifiers[0].nomisIdentifierId,
      )
      assertExpectedReferenceMapping(
        actual = response.identifiersMappings[1],
        expectedCprReferenceId = references[1].updateId.toString(),
        expectedNomisIdentifierId = requestBody.identifiers[1].nomisIdentifierId,
      )
      assertExpectedPseudonymMapping(
        actual = response.aliasesMappings[0],
        expectedCprPseudonymId = pseudonyms[0].updateId.toString(),
        expectedNomisOffenderId = requestBody.aliases[0].nomisOffenderId,
      )
      assertExpectedPseudonymMapping(
        actual = response.aliasesMappings[1],
        expectedCprPseudonymId = pseudonyms[1].updateId.toString(),
        expectedNomisOffenderId = requestBody.aliases[1].nomisOffenderId,
      )
    }

    @Test
    fun `successful save sematically identical pseudonyms`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))
      val sematicallyIdenticalPseudonym = validRequestBody().aliases.first()
      val semanticallyIdenticalPseudonymRequest = validRequestBody().copy(
        aliases = listOf(
          sematicallyIdenticalPseudonym.copy(nomisOffenderId = 10000L, isPrimary = true),
          sematicallyIdenticalPseudonym.copy(nomisOffenderId = 10001L, isPrimary = false),
        ),
      )

      val response = postAndExpect(prisonNumber, requestBody = semanticallyIdenticalPseudonymRequest)
        .isCreated
        .expectBody<SysconAliasesAndIdentifiersResponseBody>()
        .returnResult()
        .responseBody!!

      val person = personRepository.findByPrisonNumber(prisonNumber)!!
      val pseudonyms = person.pseudonyms
      // Assert that we have two pseudonyms in the database, even though they are semantically identical
      assertThat(pseudonyms[0].id).isNotEqualTo(pseudonyms[1].id) // Expect different ids
      assertPseudonymMatchesRequest(pseudonym = pseudonyms[0], request = sematicallyIdenticalPseudonym)
      assertPseudonymMatchesRequest(pseudonym = pseudonyms[1], request = sematicallyIdenticalPseudonym)

      // Assert that the mappings are as expected in the response body
      assertThat(response.aliasesMappings).hasSize(2)
      assertExpectedPseudonymMapping(
        response.aliasesMappings[0],
        expectedCprPseudonymId = pseudonyms[0].updateId.toString(),
        expectedNomisOffenderId = semanticallyIdenticalPseudonymRequest.aliases[0].nomisOffenderId,
      )
      assertExpectedPseudonymMapping(
        actual = response.aliasesMappings[1],
        expectedCprPseudonymId = pseudonyms[1].updateId.toString(),
        expectedNomisOffenderId = semanticallyIdenticalPseudonymRequest.aliases[1].nomisOffenderId,
      )
    }

    @Test
    fun `successful save deletes orphaned pseudonyms`() {
      val prisonNumber = randomPrisonNumber()
      createPerson(createRandomPrisonPersonDetails(prisonNumber))
      val pseudonymsToBeDeleted = personRepository.findByPrisonNumber(prisonNumber)!!.pseudonyms
      assertThat(pseudonymsToBeDeleted).hasSize(2)
      assertThat(pseudonymsToBeDeleted[0].id).isNotNull()
      assertThat(pseudonymsToBeDeleted[1].id).isNotNull()

      postAndExpect(prisonNumber, validRequestBody()).isCreated

      // Check that we can no longer find the orphaned pseudonyms in the repository
      val orphanedPseudonyms = pseudonymsToBeDeleted.map { pseudonymRepository.findById(it.id!!) }
      assertThat(orphanedPseudonyms).allMatch { it.isEmpty }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `should respond with bad request when no pseudonyms are posted`() {
      val noPseudonymRequestBody = validRequestBody().copy(aliases = emptyList())
      postAndExpect(randomPrisonNumber(), noPseudonymRequestBody).isBadRequest
    }

    @Test
    fun `should return bad request when there is no primary pseudonym`() {
      val noPrimaryPseudonymRequestBody =
        validRequestBody().copy(aliases = validRequestBody().aliases.map { it.copy(isPrimary = false) })
      postAndExpect(randomPrisonNumber(), noPrimaryPseudonymRequestBody).isBadRequest.expectBody()
        .jsonPath("userMessage")
        .value<String> { userMessage ->
          assertThat(userMessage).contains("There must be exactly one primary pseudonym")
        }
    }

    @Test
    fun `should return bad request when there is more than one primary pseudonym`() {
      val multiplePrimaryPseudonymRequestBody =
        validRequestBody().copy(aliases = validRequestBody().aliases.map { it.copy(isPrimary = true) })
      postAndExpect(randomPrisonNumber(), multiplePrimaryPseudonymRequestBody).isBadRequest.expectBody()
        .jsonPath("userMessage")
        .value<String> { userMessage ->
          assertThat(userMessage).contains("There must be exactly one primary pseudonym")
        }
    }

    @Test
    fun `should return bad request when there are duplicate nomis ids on the pseudonyms`() {
      val multiplePrimaryPseudonymRequestBody =
        validRequestBody().copy(aliases = validRequestBody().aliases.map { it.copy(nomisOffenderId = 10000L) })
      postAndExpect(randomPrisonNumber(), multiplePrimaryPseudonymRequestBody).isBadRequest.expectBody()
        .jsonPath("userMessage")
        .value<String> { userMessage ->
          assertThat(userMessage).contains("Duplicate nomis pseudonym ids were detected")
        }
    }

    @Test
    fun `should return bad request when there is pseudonyms without a name`() {
      val multiplePrimaryPseudonymRequestBody =
        validRequestBody().copy(aliases = listOf(validRequestBody().aliases[0].copy(firstName = null, middleNames = null, lastName = null)))
      postAndExpect(randomPrisonNumber(), multiplePrimaryPseudonymRequestBody).isBadRequest.expectBody()
        .jsonPath("userMessage")
        .value<String> { userMessage ->
          assertThat(userMessage).contains("Pseudonyms without a name were detected")
        }
    }

    @Test
    fun `should return bad request when there are duplicate nomis ids on the reference`() {
      val multiplePrimaryPseudonymRequestBody =
        validRequestBody().copy(
          identifiers = validRequestBody().identifiers.map {
            it.copy(
              nomisIdentifierId = RequestNomisIdentifierId(
                nomisOffenderId = 10000L,
                nomisSequence = 0,
              ),
            )
          },
        )
      postAndExpect(randomPrisonNumber(), multiplePrimaryPseudonymRequestBody).isBadRequest.expectBody()
        .jsonPath("userMessage")
        .value<String> { userMessage ->
          assertThat(userMessage).contains("Duplicate nomis reference ids were detected")
        }
    }

    @Test
    fun `should return bad request when there is reference without a value`() {
      val multiplePrimaryPseudonymRequestBody =
        validRequestBody().copy(identifiers = listOf(validRequestBody().identifiers[0].copy(value = "")))
      postAndExpect(randomPrisonNumber(), multiplePrimaryPseudonymRequestBody).isBadRequest.expectBody()
        .jsonPath("userMessage")
        .value<String> { userMessage ->
          assertThat(userMessage).contains("Reference without a name were detected")
        }
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.post()
        .uri(aliasesIdentifiersUrl(randomPrisonNumber()))
        .bodyValue(validRequestBody())
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

  private fun validRequestBody() = PrisonAliasesAndIdentifiersRequest(
    aliases = listOf(
      PrisonAlias(
        nomisOffenderId = 10000L,
        titleCode = TitleCode.MR,
        firstName = "firstName1",
        middleNames = "middleName1",
        lastName = "lastName1",
        dateOfBirth = LocalDate.of(1990, 1, 1),
        sexCode = SexCode.M,
        isPrimary = true,
        birthPlace = "London",
        birthCountry = CountryCode.UKR,
        ethnicity = EthnicityCode.A1,
        createDate = LocalDate.of(2020, 1, 1),
      ),
      PrisonAlias(
        nomisOffenderId = 10001L,
        titleCode = TitleCode.MR,
        firstName = "firstName2",
        middleNames = "middleName2",
        lastName = "lastName2",
        dateOfBirth = LocalDate.of(1991, 2, 2),
        sexCode = SexCode.M,
        isPrimary = false,
        birthPlace = "Sheffield",
        birthCountry = CountryCode.UKR,
        ethnicity = EthnicityCode.A2,
        createDate = LocalDate.of(2021, 2, 2),
      ),
    ),
    identifiers = listOf(
      PrisonIdentifier(
        nomisIdentifierId = RequestNomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 0),
        type = IdentifierType.PNC,
        value = "2000/1234567A",
        comment = "DVLA",
        issuedDate = LocalDate.of(2020, 1, 1),
        verified = true,
      ),
      PrisonIdentifier(
        nomisIdentifierId = RequestNomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 1),
        type = IdentifierType.PNC,
        value = "2001/1234567B",
        comment = "DVLA",
        issuedDate = LocalDate.of(2021, 2, 2),
        verified = false,
      ),
    ),
  )

  private fun assertReferenceMatchesRequest(referenceEntity: ReferenceEntity, request: PrisonIdentifier) = with(referenceEntity) {
    assertThat(identifierType).isEqualTo(request.type)
    assertThat(identifierValue).isEqualTo(request.value)
    assertThat(comment).isEqualTo(request.comment)
  }

  private fun assertPseudonymMatchesRequest(pseudonym: PseudonymEntity, request: PrisonAlias) = with(pseudonym) {
    assertThat(titleCode).isEqualTo(request.titleCode)
    assertThat(firstName).isEqualTo(request.firstName)
    assertThat(middleNames).isEqualTo(request.middleNames)
    assertThat(lastName).isEqualTo(request.lastName)
    assertThat(dateOfBirth).isEqualTo(request.dateOfBirth)
    assertThat(sexCode).isEqualTo(request.sexCode)
  }

  private fun assertExpectedReferenceMapping(
    actual: SysconIdentifierMapping,
    expectedCprReferenceId: String,
    expectedNomisIdentifierId: RequestNomisIdentifierId,
  ) = assertThat(actual).isEqualTo(
    SysconIdentifierMapping(
      nomisIdentifierId = ResponseNomisIdentifierId(
        nomisOffenderId = expectedNomisIdentifierId.nomisOffenderId,
        nomisSequence = expectedNomisIdentifierId.nomisSequence,
      ),
      cprIdentifierId = expectedCprReferenceId,
    ),
  )

  private fun postAndExpect(prisonNumber: String, requestBody: PrisonAliasesAndIdentifiersRequest) = webTestClient
    .post()
    .uri(aliasesIdentifiersUrl(prisonNumber))
    .bodyValue(requestBody)
    .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
    .exchange()
    .expectStatus()
}
