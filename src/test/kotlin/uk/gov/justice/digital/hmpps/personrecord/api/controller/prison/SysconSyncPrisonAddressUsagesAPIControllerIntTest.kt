package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAddressUsagesAPIControllerIntTest : WebTestBase() {

  @Autowired
  lateinit var addressUsageRepository: AddressUsageRepository

  @Nested
  inner class CreatePrisonerAddressUsage {

    private val validRequestBody = PrisonAddressUsage(
      nomisAddressUsageId = 10000L,
      addressUsageCode = AddressUsageCode.CURFEW,
      isActive = true,
      createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
      createUserId = "createUserId",
      modifyDateTime = LocalDateTime.of(2020, 1, 2, 12, 0),
      modifyUserId = "modifyUserId",
    )

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPostRequestAsserted<String>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    @ActiveProfiles("preprod")
    inner class PreProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPostRequestAsserted<String>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `address does not exist - returns 404 not found`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendPostRequestAsserted<String>(
          url = createPrisonerAddressUsageUrl(prisonNumber, addressId),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Address with $addressId not found")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconAddressUsageMapping>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Creation {

      @Test
      fun `successful save returns the correct response body`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val addressEntity = addressRepository.saveAndFlush(AddressEntity(person = personEntity, postcode = randomPostcode()))

        val response = sendPostRequestAsserted<SysconAddressUsageMapping>(
          url = createPrisonerAddressUsageUrl(prisonNumber, addressEntity.updateId.toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = CREATED,
        ).returnResult().responseBody!!

        val updatedAddressEntity = addressRepository.findByUpdateId(addressEntity.updateId!!)!!
        val addressUsageEntity = updatedAddressEntity.usages.single { response.cprAddressUsageId == it.updateId.toString() }
        assertThat(response.nomisAddressUsageId).isEqualTo(validRequestBody.nomisAddressUsageId)
        assertThat(response.nomisAddressUsageCode).isEqualTo(validRequestBody.addressUsageCode)
        assertAddressUsageMatches(validRequestBody, addressUsageEntity)
      }
    }
  }

  @Nested
  inner class UpdatePrisonerAddressUsage {

    private val validRequestBody = PrisonAddressUsage(
      nomisAddressUsageId = 10000L,
      addressUsageCode = AddressUsageCode.CURFEW,
      isActive = true,
      createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
      createUserId = "createUserId",
      modifyDateTime = LocalDateTime.of(2020, 1, 2, 12, 0),
      modifyUserId = "modifyUserId",
    )

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    @ActiveProfiles("preprod")
    inner class PreProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `address usage does not exist - returns 404 not found`() {
        val prisonNumber = randomPrisonNumber()
        val usageId = UUID.randomUUID().toString()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUsageUrl(prisonNumber, UUID.randomUUID().toString(), usageId),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("AddressUsage with $usageId not found")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Updating {

      @Test
      fun `successful update of an address usage`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val addressEntity = addressRepository.saveAndFlush(AddressEntity(person = personEntity, postcode = randomPostcode()))
        val addressUsageEntity = addressUsageRepository.saveAndFlush(AddressUsageEntity(address = addressEntity, usageCode = AddressUsageCode.HDC, active = true))

        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUsageUrl(prisonNumber, addressEntity.updateId.toString(), addressUsageEntity.updateId.toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )

        val updatedAddressUsageEntity = addressUsageRepository.findByUpdateId(addressUsageEntity.updateId!!)!!
        assertAddressUsageMatches(validRequestBody, updatedAddressUsageEntity)
      }
    }
  }

  @Nested
  inner class DeletePrisonerAddressUsage {

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    @ActiveProfiles("preprod")
    inner class PreProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `address does not exist - returns 404 not found`() {
        val prisonNumber = randomPrisonNumber()
        val addressId = UUID.randomUUID().toString()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUsageUrl(prisonNumber, addressId, UUID.randomUUID().toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Address with $addressId not found for person with $prisonNumber")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Deleting {

      @Test
      fun `successful delete of an address usage`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val addressEntity = addressRepository.saveAndFlush(AddressEntity(person = personEntity, postcode = randomPostcode()))
        val addressUsageEntity = addressUsageRepository.saveAndFlush(AddressUsageEntity(address = addressEntity, usageCode = AddressUsageCode.HDC, active = true))

        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUsageUrl(prisonNumber, addressEntity.updateId.toString(), addressUsageEntity.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )

        assertThat(addressUsageRepository.findById(addressUsageEntity.id!!)).isEmpty()
      }
    }
  }

  private fun createPrisonerAddressUsageUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage"
  private fun updatePrisonerAddressUsageUrl(prisonNumber: String, addressId: String, usageId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage/$usageId"
  private fun deletePrisonerAddressUsageUrl(prisonNumber: String, addressId: String, usageId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage/$usageId"

  companion object {
    fun assertAddressUsageMatches(request: PrisonAddressUsage, entity: AddressUsageEntity): Unit = with(entity) {
      assertThat(usageCode).isEqualTo(request.addressUsageCode)
      assertThat(active).isEqualTo(request.isActive)
      assertThat(modifyUserId).isEqualTo(request.modifyUserId)
      assertThat(modifyDateTime).isEqualTo(request.modifyDateTime)
      assertThat(createUserId).isEqualTo(request.createUserId)
      assertThat(createDateTime).isEqualTo(request.createDateTime)
    }
  }
}
