package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.OTHR
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonIdentifiersAPIControllerIntTest(
  @Autowired private val referenceRepository: ReferenceRepository,
) : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should update, add and remove identifiers on the alias only`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val alias = personEntity.getAliases().single()
      val originalCreateDateTime = LocalDateTime.of(2019, 6, 1, 9, 30)
      val existingPnc = saveReference(personEntity, PNC, randomLongPnc(), createDateTime = originalCreateDateTime, createUserId = "originalUser")
      val existingCro = saveReference(personEntity, CRO, randomCro())
      saveReference(personEntity, OTHR, "primary", personEntity.getPrimaryName())
      val primaryNameReferenceIds = primaryNameReferences(prisonNumber).map { it.updateId }
      val updatedPnc = randomLongPnc()

      val response = replaceIdentifiers(
        prisonNumber,
        alias.updateId.toString(),
        listOf(
          prisonIdentifier(sequence = 1, type = PNC, value = updatedPnc, cprIdentifierId = existingPnc.updateId.toString()),
          prisonIdentifier(sequence = 2, type = OTHR, value = "other"),
        ),
      )

      assertThat(response.prisonNumber).isEqualTo(prisonNumber)
      assertThat(response.identifiersMappings.map { it.nomisIdentifierId.nomisSequence }).containsExactly(1, 2)
      assertThat(response.identifiersMappings[0].cprIdentifierId).isEqualTo(existingPnc.updateId.toString())

      val aliasReferences = aliasReferences(prisonNumber)
      assertThat(aliasReferences.map { it.updateId.toString() }).containsExactlyInAnyOrderElementsOf(response.identifiersMappings.map { it.cprIdentifierId })
      assertThat(aliasReferences.map { it.identifierType to it.identifierValue }).containsExactlyInAnyOrder(PNC to updatedPnc, OTHR to "other")
      val updatedPncReference = aliasReferences.single { it.identifierType == PNC }
      assertThat(updatedPncReference.createDateTime).isEqualTo(originalCreateDateTime)
      assertThat(updatedPncReference.createUserId).isEqualTo("originalUser")
      val newOtherReference = aliasReferences.single { it.identifierType == OTHR }
      assertThat(newOtherReference.createDateTime).isEqualTo(LocalDateTime.of(2021, 1, 1, 12, 0))
      assertThat(newOtherReference.createUserId).isEqualTo("createUserId")
      assertThat(referenceRepository.findByUpdateId(existingCro.updateId!!)).isNull()
      assertThat(primaryNameReferences(prisonNumber).map { it.updateId }).containsExactlyInAnyOrderElementsOf(primaryNameReferenceIds)
    }

    @Test
    fun `should remove all identifiers on the alias when none are sent`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      saveReference(personEntity, PNC, randomLongPnc())
      saveReference(personEntity, OTHR, "primary", personEntity.getPrimaryName())

      val response = replaceIdentifiers(prisonNumber, personEntity.getAliases().single().updateId.toString(), emptyList())

      assertThat(response.identifiersMappings).isEmpty()
      assertThat(aliasReferences(prisonNumber)).isEmpty()
      assertThat(primaryNameReferences(prisonNumber)).isNotEmpty()
    }

    @Test
    fun `should format valid PNC and CRO values and store invalid values as received`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val prisonNumber = randomPrisonNumber()
      val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val shortPnc = randomShortPnc()
      val cro = randomCro()

      replaceIdentifiers(
        prisonNumber,
        personEntity.getAliases().single().updateId.toString(),
        listOf(
          prisonIdentifier(sequence = 1, type = PNC, value = shortPnc),
          prisonIdentifier(sequence = 2, type = CRO, value = cro.lowercase()),
          prisonIdentifier(sequence = 3, type = PNC, value = "invalidPnc"),
          prisonIdentifier(sequence = 4, type = CRO, value = "invalidCro"),
        ),
      )

      assertThat(aliasReferences(prisonNumber).map { it.identifierType to it.identifierValue }).containsExactlyInAnyOrder(
        PNC to PNCIdentifier.from(shortPnc).pncId,
        CRO to CROIdentifier.from(cro).croId,
        PNC to "invalidPnc",
        CRO to "invalidCro",
      )
    }

    @Test
    fun `should create a new identifier rather than move one which belongs to another name`() {
      val prisonNumber = randomPrisonNumber()
      val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val primaryNameReference = saveReference(personEntity, OTHR, "primary", personEntity.getPrimaryName())

      val response = replaceIdentifiers(
        prisonNumber,
        personEntity.getAliases().single().updateId.toString(),
        listOf(prisonIdentifier(sequence = 1, type = OTHR, value = "other", cprIdentifierId = primaryNameReference.updateId.toString())),
      )

      assertThat(response.identifiersMappings.single().cprIdentifierId).isNotEqualTo(primaryNameReference.updateId.toString())
      val unchanged = referenceRepository.findByUpdateId(primaryNameReference.updateId!!)!!
      assertThat(unchanged.identifierValue).isEqualTo(primaryNameReference.identifierValue)
      assertThat(unchanged.pseudonym?.id).isEqualTo(primaryNameReference.pseudonym?.id)
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

    @Test
    fun `should return not found when the alias does not exist`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      val cprAliasId = UUID.randomUUID().toString()

      webTestClient.put()
        .uri(identifiersUrl(prisonNumber, cprAliasId))
        .bodyValue(identifiersRequest())
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

  private fun replaceIdentifiers(prisonNumber: String, cprAliasId: String, identifiers: List<PrisonIdentifier>): SysconIdentifiersResponseBody = webTestClient.put()
    .uri(identifiersUrl(prisonNumber, cprAliasId))
    .bodyValue(PrisonIdentifiersRequest(identifiers))
    .authorised(roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
    .exchange()
    .expectStatus()
    .isOk
    .expectBody(SysconIdentifiersResponseBody::class.java)
    .returnResult()
    .responseBody!!

  private fun saveReference(
    personEntity: PersonEntity,
    type: IdentifierType,
    value: String,
    pseudonym: PseudonymEntity = personEntity.getAliases().single(),
    createDateTime: LocalDateTime? = null,
    createUserId: String? = null,
  ): ReferenceEntity = referenceRepository.saveAndFlush(
    ReferenceEntity(
      person = personEntity,
      pseudonym = pseudonym,
      identifierType = type,
      identifierValue = value,
      createDateTime = createDateTime,
      createUserId = createUserId,
    ),
  )

  private fun aliasReferences(prisonNumber: String): List<ReferenceEntity> {
    val person = personRepository.findByPrisonNumber(prisonNumber)!!
    return person.references.filter { it.pseudonym?.id == person.getAliases().single().id }
  }

  private fun primaryNameReferences(prisonNumber: String): List<ReferenceEntity> {
    val person = personRepository.findByPrisonNumber(prisonNumber)!!
    return person.references.filter { it.pseudonym?.id == person.getPrimaryName().id }
  }

  private fun prisonIdentifier(
    sequence: Int,
    type: IdentifierType = PNC,
    value: String = "2000/1234567A",
    cprIdentifierId: String? = null,
    nomisOffenderId: Long = 10000L,
  ) = PrisonIdentifier(
    nomisIdentifierId = NomisIdentifierId(nomisOffenderId = nomisOffenderId, nomisSequence = sequence),
    cprIdentifierId = cprIdentifierId,
    type = type,
    value = value,
    comment = "comment",
    verified = true,
    createDateTime = LocalDateTime.of(2021, 1, 1, 12, 0),
    createUserId = "createUserId",
  )

  private fun identifiersRequest() = PrisonIdentifiersRequest(identifiers = listOf(prisonIdentifier(sequence = 1)))

  private fun identifiersUrl(prisonNumber: String, cprAliasId: String = UUID.randomUUID().toString()) = "/syscon-sync/person/$prisonNumber/alias/$cprAliasId/identifiers"
}
