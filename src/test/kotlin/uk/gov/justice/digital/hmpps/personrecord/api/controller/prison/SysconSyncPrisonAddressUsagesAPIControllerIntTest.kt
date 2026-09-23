package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAddressUsagesAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class CreatePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPostRequestAsserted<SysconAddressUsageMapping>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconAddressUsageMapping>(
          url = createPrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class UpdatePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          body = PrisonAddressUsage(
            nomisAddressUsageId = 10000L,
            addressUsageCode = AddressUsageCode.CURFEW,
            isActive = true,
            createDateTime = LocalDateTime.parse("2020-01-01T12:00:00"),
            createUserId = "billybob",
          ),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class DeletePrisonerAddressUsage {

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUsageUrl(randomPrisonNumber(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
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
  }

  private fun createPrisonerAddressUsageUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage"
  private fun updatePrisonerAddressUsageUrl(prisonNumber: String, addressId: String, usageId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage/$usageId"
  private fun deletePrisonerAddressUsageUrl(prisonNumber: String, addressId: String, usageId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId/usage/$usageId"
}
