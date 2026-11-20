package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.config.TransactionalLookup
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode.UKR
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.A1
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType.CN
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix.I
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode.MR
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode.SIR
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID.randomUUID

class SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest : WebTestBase() {

  @Autowired
  lateinit var pseudonymRepository: PseudonymRepository

  @Autowired
  lateinit var prisonPseudonymRepository: PrisonPseudonymRepository

  @Autowired
  lateinit var referenceRepository: ReferenceRepository

  @Autowired
  lateinit var transactionalLookup: TransactionalLookup

  @Nested
  @ActiveProfiles("prod")
  inner class ProductionProfile {

    @Test
    fun `should not be available in prod`() {
      val prisonNumber = randomPrisonNumber()
      val body = sendPostRequestAsserted<String>(
        url = pseudonymReferenceUrl(prisonNumber),
        body = validPrisonPseudoymnAndReferencesRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = NOT_FOUND,
      ).returnResult().responseBody!!
      assertThat(body).contains("\"error\":\"Not Found\"")
    }
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class PreProductionProfile {

    @Test
    fun `should not be available in preprod`() {
      val body = sendPostRequestAsserted<String>(
        url = pseudonymReferenceUrl(randomPrisonNumber()),
        body = validPrisonPseudoymnAndReferencesRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = NOT_FOUND,
      ).returnResult().responseBody!!
      assertThat(body).contains("\"error\":\"Not Found\"")
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    private fun badRequest(request: PrisonAliasesAndIdentifiersRequest): String {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      return sendPostRequestAsserted<String>(
        url = pseudonymReferenceUrl(prisonNumber),
        body = request,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = BAD_REQUEST,
      ).returnResult().responseBody!!
    }

    @Test
    fun `should return not found when the person does not exist`() {
      sendPostRequestAsserted<String>(
        url = pseudonymReferenceUrl(randomPrisonNumber()),
        body = validPrisonPseudoymnAndReferencesRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = NOT_FOUND,
      )
    }

    @Test
    fun `should return bad request when pseudonyms are empty`() {
      badRequest(PrisonAliasesAndIdentifiersRequest(aliases = emptyList()))
    }

    @Test
    fun `should return bad request when there are duplicate nomis offender ids on the pseudonyms`() {
      val duplicatePseudonymIdsOnRequestBody = validPrisonPseudoymnAndReferencesRequest.copy(aliases = validPrisonPseudoymnAndReferencesRequest.aliases.map { it.copy(nomisOffenderId = 10000L, identifiers = emptyList()) })
      assertThat(badRequest(duplicatePseudonymIdsOnRequestBody)).contains("Duplicate nomis pseudonym offender ids were detected")
    }

    @Test
    fun `should return bad request when there are multiple primary pseudonyms`() {
      val multiplePrimaryPseudonymsRequestBody = validPrisonPseudoymnAndReferencesRequest.copy(aliases = validPrisonPseudoymnAndReferencesRequest.aliases.map { it.copy(isPrimary = true) })
      assertThat(badRequest(multiplePrimaryPseudonymsRequestBody)).contains("There must be exactly one primary pseudonym")
    }

    @Test
    fun `should return bad request when there is no primary pseudonym`() {
      val noPrimaryPseudonymsRequestBody = validPrisonPseudoymnAndReferencesRequest.copy(aliases = validPrisonPseudoymnAndReferencesRequest.aliases.map { it.copy(isPrimary = false) })
      assertThat(badRequest(noPrimaryPseudonymsRequestBody)).contains("There must be exactly one primary pseudonym")
    }

    @Test
    fun `should return bad request when an reference does not match its parent nomis pseudonym offender id`() {
      val referenceDoesNotMatchPseudonymRequestBody = PrisonAliasesAndIdentifiersRequest(
        aliases = listOf(
          PrisonAlias(
            nomisOffenderId = 99999L,
            isPrimary = true,
            createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
            createUserId = "createUserId",
            identifiers = listOf(
              PrisonIdentifier(
                nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 1),
                type = PNC,
                value = "2001/2186242P",
                verified = true,
                createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
                createUserId = "createUserId",
              ),
            ),
          ),
        ),
      )
      assertThat(badRequest(referenceDoesNotMatchPseudonymRequestBody)).contains("Incorrect nomis reference ids were detected")
    }

    @Test
    fun `should return bad request when there are duplicate nomis reference ids`() {
      val multipleDuplicateReferenceIdRequestBody = PrisonAliasesAndIdentifiersRequest(
        aliases = listOf(
          PrisonAlias(
            nomisOffenderId = 99999L,
            isPrimary = true,
            createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
            createUserId = "createUserId",
            identifiers = listOf(
              PrisonIdentifier(
                nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 99999L, nomisSequence = 1),
                type = PNC,
                value = "2002/2648976B",
                verified = true,
                createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
                createUserId = "createUserId",
              ),
              PrisonIdentifier(
                nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 99999L, nomisSequence = 1),
                type = CRO,
                value = "543685/62Q",
                verified = true,
                createDateTime = LocalDateTime.of(2021, 1, 1, 12, 0),
                createUserId = "createUserId",
              ),
            ),
          ),
        ),
      )
      assertThat(badRequest(multipleDuplicateReferenceIdRequestBody)).contains("Duplicate nomis reference ids were detected")
    }
  }

  @Nested
  inner class Auth {

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<String>(
        url = pseudonymReferenceUrl(randomPrisonNumber()),
        body = validPrisonPseudoymnAndReferencesRequest,
        roles = listOf("UNSUPPORTED-ROLE"),
        expectedStatus = FORBIDDEN,
      )
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      webTestClient.post()
        .uri(pseudonymReferenceUrl(randomPrisonNumber()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class Creation {

    @Test
    fun `successful save returns the correct response body and persists pseudonyms and references`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val response = postPseudonymsAndReferences(prisonNumber, validPrisonPseudoymnAndReferencesRequest)

      val personEntity = transactionalLookup.findByPrisonNumberWithReferences(prisonNumber)

      for (pseudonymRequest in validPrisonPseudoymnAndReferencesRequest.aliases) {
        // Because ordering is not guaranteed, we need to find the matching entity for each request
        val pseudonymEntity = personEntity.pseudonyms.single { pseudonymMatcher(pseudonymRequest, it) }
        assertPseudonymMatches(pseudonymRequest, pseudonymEntity)

        val pseudonymMapping = response.aliasesMappings.single { pseudonymMatcher(pseudonymRequest, it) }
        assertThat(pseudonymMapping.cprAliasId).isEqualTo(pseudonymEntity.updateId.toString())
        assertThat(pseudonymMapping.identifierMappings).hasSize(pseudonymRequest.identifiers.size)

        for (referenceRequest in pseudonymRequest.identifiers) {
          val referenceEntity = pseudonymEntity.references.single { referenceMatcher(referenceRequest, it) }
          assertReferenceMatches(referenceRequest, referenceEntity)
          assertThat(referenceEntity.person!!.id).isEqualTo(personEntity.id)
          assertThat(referenceEntity.pseudonym!!.id).isEqualTo(pseudonymEntity.id)

          val referenceMapping = pseudonymMapping.identifierMappings.single { referenceMatcher(referenceRequest, it) }
          assertThat(referenceMapping.cprIdentifierId).isEqualTo(referenceEntity.updateId.toString())
        }
      }
    }

    @Test
    fun `successful save deletes orphaned pseudonyms and references`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      // Best way to check is to migrate twice and check that the first migration does not persist.
      // First request.
      postPseudonymsAndReferences(prisonNumber, validPrisonPseudoymnAndReferencesRequest)
      val person = transactionalLookup.findByPrisonNumberWithReferences(prisonNumber)
      val pseudonymsToBeDeleted = person.pseudonyms
      val referencesToBeDeleted = person.pseudonyms.flatMap { it.references }
      assertThat(pseudonymsToBeDeleted).isNotEmpty()
      assertThat(referencesToBeDeleted).isNotEmpty()
      // Second request. We use the same request, this should still result in the old pseudonyms and references being deleted
      postPseudonymsAndReferences(prisonNumber, validPrisonPseudoymnAndReferencesRequest)

      // Check we cannot find the old pseudonyms and references
      for (pseudonym in pseudonymsToBeDeleted) {
        assertThat(pseudonymRepository.findById(pseudonym.id!!)).isEmpty()
      }
      for (reference in referencesToBeDeleted) {
        assertThat(referenceRepository.findById(reference.id!!)).isEmpty()
      }
    }

    @Test
    fun `successful save with existing cpr ids updates in place`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      // Best way to check is to migrate twice and use the update ids from the first request in the second.
      val firstResponse = postPseudonymsAndReferences(prisonNumber, validPrisonPseudoymnAndReferencesRequest)

      // Generate a second migrate that uses the update ids returned from the first request.
      val requestWithCprUpdateIds = PrisonAliasesAndIdentifiersRequest(
        aliases = validPrisonPseudoymnAndReferencesRequest.aliases.map { pseudonym ->
          val pseudonymMapping = firstResponse.aliasesMappings.single { pseudonymMatcher(pseudonym, it) }
          pseudonym.copy(
            cprAliasId = pseudonymMapping.cprAliasId,
            identifiers = pseudonym.identifiers.map { identifier ->
              identifier.copy(
                cprIdentifierId = pseudonymMapping.identifierMappings.single { referenceMatcher(identifier, it) }.cprIdentifierId,
              )
            },
          )
        },
      )

      // Second request.
      val secondResponse = postPseudonymsAndReferences(prisonNumber, requestWithCprUpdateIds)
      assertThat(secondResponse.aliasesMappings).containsExactlyInAnyOrderElementsOf(firstResponse.aliasesMappings)
    }

    @Test
    fun `successful save ignores pseudonyms and references ids that no longer exist and creates new ones`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val requestWithCprUpdateIdsThatDoNotExist = PrisonAliasesAndIdentifiersRequest(
        aliases = validPrisonPseudoymnAndReferencesRequest.aliases.map { pseudonym ->
          pseudonym.copy(
            cprAliasId = randomUUID().toString(),
            identifiers = pseudonym.identifiers.map { identifier ->
              identifier.copy(
                cprIdentifierId = randomUUID().toString(),
              )
            },
          )
        },
      )

      val response = postPseudonymsAndReferences(prisonNumber, requestWithCprUpdateIdsThatDoNotExist)
      assertThat(response.aliasesMappings).hasSize(requestWithCprUpdateIdsThatDoNotExist.aliases.size)
      assertThat(response.aliasesMappings.flatMap { it.identifierMappings }).hasSize(requestWithCprUpdateIdsThatDoNotExist.aliases.flatMap { it.identifiers }.size)
    }

    private fun postPseudonymsAndReferences(prisonNumber: String, request: PrisonAliasesAndIdentifiersRequest) = sendPostRequestAsserted<SysconAliasesAndIdentifiersResponseBody>(
      url = pseudonymReferenceUrl(prisonNumber),
      body = request,
      roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
      expectedStatus = CREATED,
    ).returnResult().responseBody!!
  }

  private fun pseudonymReferenceUrl(prisonNumber: String) = "/syscon-sync/aliases-identifiers/$prisonNumber"

  private fun pseudonymMatcher(request: PrisonAlias, entity: PseudonymEntity) = entity.firstName == request.firstName

  private fun pseudonymMatcher(request: PrisonAlias, mapping: SysconAliasMapping) = mapping.nomisOffenderId == request.nomisOffenderId

  private fun referenceMatcher(request: PrisonIdentifier, entity: ReferenceEntity) = entity.identifierValue == request.value

  private fun referenceMatcher(request: PrisonIdentifier, mapping: SysconIdentifierMapping) = mapping.nomisIdentifierId.nomisOffenderId == request.nomisIdentifierId.nomisOffenderId &&
    mapping.nomisIdentifierId.nomisSequence == request.nomisIdentifierId.nomisSequence

  private fun assertPseudonymMatches(request: PrisonAlias, entity: PseudonymEntity) {
    assertThat(entity.nameType).isEqualTo(if (request.isPrimary) NameType.PRIMARY else NameType.ALIAS)
    assertThat(entity.titleCode).isEqualTo(request.titleCode)
    assertThat(entity.firstName).isEqualTo(request.firstName)
    assertThat(entity.middleNames).isEqualTo(request.middleNames)
    assertThat(entity.lastName).isEqualTo(request.lastName)
    assertThat(entity.dateOfBirth).isEqualTo(request.dateOfBirth)
    assertThat(entity.sexCode).isEqualTo(request.sexCode)
    assertThat(entity.ethnicityCode).isEqualTo(request.ethnicity)
    assertThat(entity.birthplace).isEqualTo(request.birthPlace)
    assertThat(entity.birthCountryCode).isEqualTo(request.birthCountry)
    assertThat(entity.createDateTime).isEqualTo(request.createDateTime)
    assertThat(entity.createUserId).isEqualTo(request.createUserId)
    assertThat(entity.modifyDateTime).isEqualTo(request.modifyDateTime)
    assertThat(entity.modifyUserId).isEqualTo(request.modifyUserId)
  }

  private fun assertReferenceMatches(request: PrisonIdentifier, entity: ReferenceEntity) {
    assertThat(entity.identifierType).isEqualTo(request.type)
    assertThat(entity.identifierValue).isEqualTo(request.value)
    assertThat(entity.comment).isEqualTo(request.comment)
    assertThat(entity.isVerified).isEqualTo(request.verified)
    assertThat(entity.createDateTime).isEqualTo(request.createDateTime)
    assertThat(entity.createUserId).isEqualTo(request.createUserId)
    assertThat(entity.modifyDateTime).isEqualTo(request.modifyDateTime)
    assertThat(entity.modifyUserId).isEqualTo(request.modifyUserId)
  }

  companion object {

    val validPrisonPseudoymnAndReferencesRequest = PrisonAliasesAndIdentifiersRequest(
      aliases = listOf(
        PrisonAlias(
          nomisOffenderId = 10000L,
          titleCode = MR,
          firstName = "firstName1",
          middleNames = "middleNames1",
          lastName = "lastName1",
          dateOfBirth = LocalDate.of(1990, 1, 1),
          sexCode = M,
          isPrimary = true,
          birthPlace = "London",
          birthCountry = UKR,
          ethnicity = A1,
          suffix = I,
          nameType = CN,
          createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
          createUserId = "createUserIdAlias1",
          modifyDateTime = LocalDateTime.of(2020, 1, 2, 12, 0),
          modifyUserId = "modifyUserIdAlias1",
          identifiers = listOf(
            PrisonIdentifier(
              nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 1),
              type = PNC,
              value = "1988/3461238",
              comment = "comment1",
              issuedDate = LocalDate.of(2020, 1, 1),
              verified = true,
              createDateTime = LocalDateTime.of(2021, 1, 1, 12, 0),
              createUserId = "createUserIdIdentifier1",
              modifyDateTime = LocalDateTime.of(2021, 1, 2, 12, 0),
              modifyUserId = "modifyUserIdIdentifier1",
            ),
            PrisonIdentifier(
              nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 10000L, nomisSequence = 2),
              type = CRO,
              value = "959442/89B",
              comment = "comment2",
              issuedDate = null,
              verified = false,
              createDateTime = LocalDateTime.of(2021, 2, 1, 12, 0),
              createUserId = "createUserIdIdentifier2",
            ),
          ),
        ),
        PrisonAlias(
          nomisOffenderId = 10001L,
          titleCode = SIR,
          firstName = "firstName2",
          middleNames = "middleNames2",
          lastName = "lastName2",
          dateOfBirth = LocalDate.of(1991, 2, 2),
          sexCode = M,
          isPrimary = false,
          birthPlace = null,
          birthCountry = null,
          ethnicity = null,
          suffix = null,
          nameType = ALIAS,
          createDateTime = LocalDateTime.of(2022, 1, 1, 12, 0),
          createUserId = "createUserIdAlias2",
          identifiers = listOf(
            PrisonIdentifier(
              nomisIdentifierId = NomisIdentifierId(nomisOffenderId = 10001L, nomisSequence = 1),
              type = PNC,
              value = "1959/7517495M",
              issuedDate = null,
              verified = true,
              createDateTime = LocalDateTime.of(2022, 2, 1, 12, 0),
              createUserId = "createUserIdIdentifier3",
            ),
          ),
        ),
      ),
    )
  }
}
