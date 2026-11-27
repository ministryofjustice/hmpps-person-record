package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.Companion.validPrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest.Companion.validPrisonPseudoymnAndReferencesRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.SyncReconciliation
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class SyncReconciliationAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should return aliases and identifiers with cpr ids`() {
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

      val response = getReconciliation(prisonNumber)

      aliasMappings.forEach { aliasMapping ->
        val aliasRequest = aliasesRequest.aliases.single { it.nomisOffenderId == aliasMapping.nomisOffenderId }
        val aliasResponse = response.aliases.single { it.cprAliasId == aliasMapping.cprAliasId }
        assertThat(aliasResponse)
          .usingRecursiveComparison()
          .ignoringFields("nomisOffenderId", "cprAliasId", "identifiers")
          .isEqualTo(aliasRequest)

        aliasMapping.identifierMappings.forEach { identifierMapping ->
          val identifierRequest = aliasRequest.identifiers.single {
            it.nomisIdentifierId!!.nomisOffenderId == identifierMapping.nomisIdentifierId.nomisOffenderId &&
              it.nomisIdentifierId.nomisSequence == identifierMapping.nomisIdentifierId.nomisSequence
          }
          val identifierResponse = aliasResponse.identifiers.single { it.cprIdentifierId == identifierMapping.cprIdentifierId }
          assertThat(identifierResponse)
            .usingRecursiveComparison()
            .ignoringFields("nomisIdentifierId", "cprIdentifierId")
            .isEqualTo(identifierRequest)
        }
      }
    }

    @Test
    fun `should return addresses, address usages and contacts with cpr ids`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val addressesRequest = validPrisonAddressesAndContactsRequest
      val addressesMappings = sendPostRequestAsserted<SysconAddressesAndContactsResponseBody>(
        url = "/syscon-sync/addresses-contacts/$prisonNumber",
        body = addressesRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!

      val response = getReconciliation(prisonNumber)

      assertThat(response.prisonNumber).isEqualTo(prisonNumber)
      assertThat(response.addresses).hasSameSizeAs(addressesRequest.addresses)
      addressesMappings.addressesMappings.forEach { addressMapping ->
        val addressRequest = addressesRequest.addresses.single { it.nomisAddressId == addressMapping.nomisAddressId }
        val addressResponse = response.addresses.single { it.cprAddressId == addressMapping.cprAddressId }
        assertThat(addressResponse)
          .usingRecursiveComparison()
          .ignoringFields("nomisAddressId", "cprAddressId", "addressUsage", "contacts")
          .isEqualTo(addressRequest)

        assertThat(addressResponse.addressUsage).hasSameSizeAs(addressRequest.addressUsage)
        addressMapping.addressUsageMappings.forEach { usageMapping ->
          val usageRequest = addressRequest.addressUsage.single {
            it.nomisAddressUsageId == usageMapping.nomisAddressUsageId && it.addressUsageCode == usageMapping.nomisAddressUsageCode
          }
          val usageResponse = addressResponse.addressUsage.single { it.cprAddressUsageId == usageMapping.cprAddressUsageId }
          assertThat(usageResponse)
            .usingRecursiveComparison()
            .ignoringFields("nomisAddressUsageId", "cprAddressUsageId")
            .isEqualTo(usageRequest)
        }

        assertThat(addressResponse.contacts).hasSameSizeAs(addressRequest.contacts)
        addressMapping.contactMappings.forEach { contactMapping ->
          val contactRequest = addressRequest.contacts.single { it.nomisContactId == contactMapping.nomisContactId }
          val contactResponse = addressResponse.contacts.single { it.cprContactId == contactMapping.cprContactId }
          assertThat(contactResponse.cprAddressId).isEqualTo(addressMapping.cprAddressId)
          assertThat(contactResponse)
            .usingRecursiveComparison()
            .ignoringFields("nomisContactId", "cprContactId", "cprAddressId", "prisonNumber")
            .isEqualTo(contactRequest)
        }
      }

      assertThat(response.contacts).hasSameSizeAs(addressesRequest.contacts)
      addressesMappings.contactMappings.forEach { contactMapping ->
        val contactRequest = addressesRequest.contacts.single { it.nomisContactId == contactMapping.nomisContactId }
        val contactResponse = response.contacts.single { it.cprContactId == contactMapping.cprContactId }
        assertThat(contactResponse.prisonNumber).isEqualTo(prisonNumber)
        assertThat(contactResponse)
          .usingRecursiveComparison()
          .ignoringFields("nomisContactId", "cprContactId", "cprAddressId", "prisonNumber")
          .isEqualTo(contactRequest)
      }
    }

    @Test
    fun `should return religion history`() {
      val prisonNumber = randomPrisonNumber()
      createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))

      val religionsRequest = PrisonReligionRequest(listOf(createPrisonReligionHistory(current = true), createPrisonReligionHistory(current = false)))
      val religionMappings = sendPostRequestAsserted<SysconReligionResponseBody>(
        url = "/syscon-sync/religion/$prisonNumber",
        body = religionsRequest,
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = CREATED,
      ).returnResult().responseBody!!.religionMappings

      val response = getReconciliation(prisonNumber)

      assertThat(response.prisonNumber).isEqualTo(prisonNumber)
      assertThat(response.religions).hasSameSizeAs(religionsRequest.religions)
      religionMappings.forEach { religionMapping ->
        val religionRequest = religionsRequest.religions.first { it.nomisReligionId == religionMapping.nomisReligionId }
        val religionResponse = response.religions.first { it.cprReligionId == religionMapping.cprReligionId }
        assertThat(religionResponse)
          .usingRecursiveComparison()
          .ignoringFields("religionDescription", "cprReligionId")
          .isEqualTo(religionRequest)
      }
    }
  }

  @Nested
  inner class Validation {

    @Test
    fun `should return not found 404 when the prison number does not exist`() {
      val prisonNumber = randomPrisonNumber()
      val response = sendGetRequestAsserted<String>(
        url = reconciliationUrl(prisonNumber),
        roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
        expectedStatus = NOT_FOUND,
      ).returnResult().responseBody!!
      assertThat(response).contains("Not found: $prisonNumber")
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
