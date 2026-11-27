package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.Companion.validPrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest.Companion.validPrisonPseudoymnAndReferencesRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.SyncReconciliation
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SyncReconciliationAPIControllerIntTest : WebTestBase() {

  @Autowired
  private lateinit var prisonReligionRepository: PrisonReligionRepository

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return everything synchronised from nomis with cpr ids and no nomis ids`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val aliasesRequest = validPrisonPseudoymnAndReferencesRequest
      val aliasMappings = sendPostRequestAsserted<SysconAliasesAndIdentifiersResponseBody>(
        url = "/syscon-sync/aliases-identifiers/$prisonNumber",
        body = aliasesRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!.aliasesMappings

      val addressesRequest = validPrisonAddressesAndContactsRequest
      val addressesMappings = sendPostRequestAsserted<SysconAddressesAndContactsResponseBody>(
        url = "/syscon-sync/addresses-contacts/$prisonNumber",
        body = addressesRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      val religionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonNumber, createPrisonReligionHistory()))

      val response = getReconciliation(prisonNumber)

      assertThat(response.prisonNumber).isEqualTo(prisonNumber)

      assertThat(response.aliases).hasSameSizeAs(aliasesRequest.aliases)
      aliasesRequest.aliases.forEach { aliasRequest ->
        val aliasMapping = aliasMappings.single { it.nomisOffenderId == aliasRequest.nomisOffenderId }
        val alias = response.aliases.single { it.cprAliasId == aliasMapping.cprAliasId }
        assertThat(alias.nomisOffenderId).isNull()
        assertThat(alias)
          .usingRecursiveComparison()
          .ignoringFields("nomisOffenderId", "cprAliasId", "identifiers")
          .isEqualTo(aliasRequest)

        assertThat(alias.identifiers).hasSameSizeAs(aliasRequest.identifiers)
        aliasRequest.identifiers.forEach { identifierRequest ->
          val identifierMapping = aliasMapping.identifierMappings.single {
            it.nomisIdentifierId.nomisOffenderId == identifierRequest.nomisIdentifierId?.nomisOffenderId &&
              it.nomisIdentifierId.nomisSequence == identifierRequest.nomisIdentifierId?.nomisSequence
          }
          val identifier = alias.identifiers.single { it.cprIdentifierId == identifierMapping.cprIdentifierId }
          assertThat(identifier.nomisIdentifierId).isNull()
          assertThat(identifier)
            .usingRecursiveComparison()
            .ignoringFields("nomisIdentifierId", "cprIdentifierId")
            .isEqualTo(identifierRequest)
        }
      }

      assertThat(response.addresses).hasSameSizeAs(addressesRequest.addresses)
      addressesRequest.addresses.forEach { addressRequest ->
        val addressMapping = addressesMappings.addressesMappings.single { it.nomisAddressId == addressRequest.nomisAddressId }
        val address = response.addresses.single { it.cprAddressId == addressMapping.cprAddressId }
        assertThat(address.nomisAddressId).isNull()
        assertThat(address)
          .usingRecursiveComparison()
          .ignoringFields("nomisAddressId", "cprAddressId", "addressUsage", "contacts")
          .isEqualTo(addressRequest)

        assertThat(address.addressUsage).hasSameSizeAs(addressRequest.addressUsage)
        addressRequest.addressUsage.forEach { usageRequest ->
          val usageMapping = addressMapping.addressUsageMappings.single {
            it.nomisAddressUsageId == usageRequest.nomisAddressUsageId && it.nomisAddressUsageCode == usageRequest.addressUsageCode
          }
          val usage = address.addressUsage.single { it.cprAddressUsageId == usageMapping.cprAddressUsageId }
          assertThat(usage.nomisAddressUsageId).isNull()
          assertThat(usage)
            .usingRecursiveComparison()
            .ignoringFields("nomisAddressUsageId", "cprAddressUsageId")
            .isEqualTo(usageRequest)
        }

        assertThat(address.contacts).hasSameSizeAs(addressRequest.contacts)
        addressRequest.contacts.forEach { contactRequest ->
          val contactMapping = addressMapping.contactMappings.single { it.nomisContactId == contactRequest.nomisContactId }
          val contact = address.contacts.single { it.cprContactId == contactMapping.cprContactId }
          assertThat(contact.nomisContactId).isNull()
          assertThat(contact.cprAddressId).isEqualTo(addressMapping.cprAddressId)
          assertThat(contact)
            .usingRecursiveComparison()
            .ignoringFields("nomisContactId", "cprContactId", "cprAddressId", "prisonNumber")
            .isEqualTo(contactRequest)
        }
      }

      assertThat(response.contacts).hasSameSizeAs(addressesRequest.contacts)
      addressesRequest.contacts.forEach { contactRequest ->
        val contactMapping = addressesMappings.contactMappings.single { it.nomisContactId == contactRequest.nomisContactId }
        val contact = response.contacts.single { it.cprContactId == contactMapping.cprContactId }
        assertThat(contact.nomisContactId).isNull()
        assertThat(contact.prisonNumber).isEqualTo(prisonNumber)
        assertThat(contact)
          .usingRecursiveComparison()
          .ignoringFields("nomisContactId", "cprContactId", "cprAddressId", "prisonNumber")
          .isEqualTo(contactRequest)
      }

      assertThat(response.religions).containsExactly(PrisonReligion.from(religionEntity))
    }

    @Test
    fun `should return empty lists when only names have been synchronised`() {
      val prisonNumber = randomPrisonNumber()
      val person = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber).copy(addresses = mutableListOf()))

      val response = getReconciliation(prisonNumber)

      assertThat(response.aliases).hasSize(person.pseudonyms.size)
      assertThat(response.aliases.single { it.isPrimary }.cprAliasId).isEqualTo(person.getPrimaryName().updateId.toString())
      assertThat(response.addresses).isEmpty()
      assertThat(response.contacts).isEmpty()
      assertThat(response.religions).isEmpty()
    }
  }

  @Nested
  inner class ErrorScenarios {

    @Test
    fun `should return not found 404 when the prison number does not exist`() {
      val prisonNumber = randomPrisonNumber()
      webTestClient.get()
        .uri(reconciliationUrl(prisonNumber))
        .authorised(listOf(PERSON_RECORD_SYSCON_SYNC_WRITE))
        .exchange()
        .expectStatus()
        .isEqualTo(NOT_FOUND)
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Not found: $prisonNumber")
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendGetRequestAsserted<Unit>(
        url = reconciliationUrl(randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = FORBIDDEN,
      )
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendGetRequestAsserted<Unit>(
        url = reconciliationUrl(randomPrisonNumber()),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = UNAUTHORIZED,
        sendAuthorised = false,
      )
    }
  }

  private fun getReconciliation(prisonNumber: String) = sendGetRequestAsserted<SyncReconciliation>(
    url = reconciliationUrl(prisonNumber),
    roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
    expectedStatus = OK,
  ).returnResult().responseBody!!

  private fun reconciliationUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/reconciliation"
}
