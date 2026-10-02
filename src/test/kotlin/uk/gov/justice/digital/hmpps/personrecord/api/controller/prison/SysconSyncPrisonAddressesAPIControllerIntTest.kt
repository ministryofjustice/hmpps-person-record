package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAddressesAPIControllerIntTest : WebTestBase() {

  @Nested
  inner class CreatePrisonerAddress {

    private val validRequestBody = PrisonAddress(
      nomisAddressId = 10000L,
      fullAddress = "fullAddress",
      noFixedAbode = false,
      startDate = LocalDate.of(2020, 1, 1),
      endDate = LocalDate.of(2023, 7, 15),
      postcode = "SW1H 9AJ",
      subBuildingName = "subBuildingName",
      buildingName = "buildingName",
      buildingNumber = "102",
      thoroughfareName = "thoroughfareName",
      dependentLocality = "dependentLocality",
      postTown = "postTown",
      county = "county",
      countryCode = CountryCode.GBR,
      comment = "comment",
      isPrimary = true,
      isMail = false,
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
          url = createPrisonerAddressUrl(randomPrisonNumber()),
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
          url = createPrisonerAddressUrl(randomPrisonNumber()),
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
      fun `person does not exist - returns 404 not found`() {
        val prisonNumber = randomPrisonNumber()
        val response = sendPostRequestAsserted<String>(
          url = createPrisonerAddressUrl(prisonNumber),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Not found: Person with $prisonNumber not found")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = createPrisonerAddressUrl(randomPrisonNumber()),
          body = validRequestBody,
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconAddressMapping>(
          url = createPrisonerAddressUrl(randomPrisonNumber()),
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
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendPostRequestAsserted<SysconAddressMapping>(
          url = createPrisonerAddressUrl(prisonNumber),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = CREATED,
        ).returnResult().responseBody!!

        assertThat(response.nomisAddressId).isEqualTo(validRequestBody.nomisAddressId)
        val addressEntity = addressRepository.findByUpdateId(UUID.fromString(response.cprAddressId))
        assertAddressMatches(validRequestBody, addressEntity!!)
      }
    }
  }

  @Nested
  inner class UpdatePrisonerAddress {

    private val validRequestBody = PrisonAddress(
      nomisAddressId = 10000L,
      fullAddress = "fullAddress",
      noFixedAbode = false,
      startDate = LocalDate.of(2020, 1, 1),
      endDate = LocalDate.of(2023, 7, 15),
      postcode = "SW1H 9AJ",
      subBuildingName = "subBuildingName",
      buildingName = "buildingName",
      buildingNumber = "102",
      thoroughfareName = "thoroughfareName",
      dependentLocality = "dependentLocality",
      postTown = "postTown",
      county = "county",
      countryCode = CountryCode.GBR,
      comment = "comment",
      isPrimary = true,
      isMail = false,
      createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
      createUserId = "createUserId",
      modifyDateTime = LocalDateTime.of(2020, 1, 2, 12, 0),
      modifyUserId = "modifyUserId",
    )

    @Nested
    inner class Validation {

      @Test
      fun `should respond with 501 as not currently implemented`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_IMPLEMENTED,
        )
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  inner class DeletePrisonerAddress {

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Deleting {

      @Test
      fun `successful delete of an address`() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        val person = createRandomPrisonPersonDetails(prisonNumber).copy(
          addresses = listOf(
            Address(postcode = randomPostcode()),
            Address(postcode = randomPostcode()),
          ),
        )
        val personEntity = createPersonWithNewKey(person)
        val addressToDelete = personEntity.addresses.first()

        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerAddressUrl(prisonNumber, addressToDelete.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )

        assertThat(addressRepository.findById(addressToDelete.id!!)).isEmpty()
      }
    }
  }
  companion object {
    fun assertAddressMatches(request: PrisonAddress, entity: AddressEntity): Unit = with(entity) {
      assertThat(fullAddress).isEqualTo(request.fullAddress)
      assertThat(noFixedAbode).isEqualTo(request.noFixedAbode)
      assertThat(startDate?.toUkLocalDate()).isEqualTo(request.startDate)
      assertThat(endDate?.toUkLocalDate()).isEqualTo(request.endDate)
      assertThat(postcode).isEqualTo(request.postcode)
      assertThat(subBuildingName).isEqualTo(request.subBuildingName)
      assertThat(buildingName).isEqualTo(request.buildingName)
      assertThat(buildingNumber).isEqualTo(request.buildingNumber)
      assertThat(thoroughfareName).isEqualTo(request.thoroughfareName)
      assertThat(dependentLocality).isEqualTo(request.dependentLocality)
      assertThat(postTown).isEqualTo(request.postTown)
      assertThat(county).isEqualTo(request.county)
      assertThat(countryCode).isEqualTo(request.countryCode)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(statusCode).isEqualTo(AddressStatusCode.fromPrison(request.isPrimary, request.isMail ?: false))
      assertThat(createDateTime).isEqualTo(request.createDateTime)
      assertThat(createUserId).isEqualTo(request.createUserId)
      assertThat(modifyDateTime).isEqualTo(request.modifyDateTime)
      assertThat(modifyUserId).isEqualTo(request.modifyUserId)
    }
  }

  private fun createPrisonerAddressUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/address"
  private fun updatePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
  private fun deletePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
}
