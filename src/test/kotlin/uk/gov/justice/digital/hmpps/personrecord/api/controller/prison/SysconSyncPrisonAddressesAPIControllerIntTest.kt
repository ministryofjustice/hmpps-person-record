package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonAddressesAPIControllerIntTest : WebTestBase() {

  @Autowired
  lateinit var addressUsageRepository: AddressUsageRepository

  @Autowired
  lateinit var contactRepository: ContactRepository

  @Nested
  inner class GetPrisonerAddress {

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendGetRequestAsserted<String>(
          url = getPrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        val response = sendGetRequestAsserted<String>(
          url = getPrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        val response = sendGetRequestAsserted<String>(
          url = getPrisonerAddressUrl(prisonNumber, addressId),
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
        sendGetRequestAsserted<String>(
          url = getPrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<PrisonAddress>(
          url = getPrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Retrieval {

      @Test
      fun `successful get returns the correct response body`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val addressEntity = addressRepository.saveAndFlush(
          AddressEntity(
            fullAddress = "fullAddress",
            noFixedAbode = false,
            startDate = LocalDate.of(2020, 1, 1).toUkZonedDateTime(),
            endDate = LocalDate.of(2023, 7, 15).toUkZonedDateTime(),
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
            createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
            createUserId = "createUserId",
            modifyDateTime = LocalDateTime.of(2020, 1, 2, 12, 0),
            modifyUserId = "modifyUserId",
            statusCode = AddressStatusCode.PM,
            person = personEntity,
          ),
        )

        val response = sendGetRequestAsserted<PrisonAddress>(
          url = getPrisonerAddressUrl(prisonNumber, addressEntity.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = OK,
        ).returnResult().responseBody!!

        assertAddressMatches(response, addressEntity)
      }

      @Test
      fun `successful get of a mail only address returns the correct primary and mail flags`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        fun addressEntity(addressStatusCode: AddressStatusCode?) = AddressEntity(
          statusCode = addressStatusCode,
          person = personEntity,
          createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
          createUserId = "createUserId",
        )
        val addressEntityPrimaryMail = addressRepository.saveAndFlush(addressEntity(AddressStatusCode.PM))
        val responsePrimaryMail = sendGetRequestAsserted<PrisonAddress>(
          url = getPrisonerAddressUrl(prisonNumber, addressEntityPrimaryMail.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = OK,
        ).returnResult().responseBody!!
        assertThat(responsePrimaryMail.isPrimary).isTrue()
        assertThat(responsePrimaryMail.isMail).isTrue()

        val addressEntityPrimaryNotMail = addressRepository.saveAndFlush(addressEntity(AddressStatusCode.M))
        val responsePrimaryNotMail = sendGetRequestAsserted<PrisonAddress>(
          url = getPrisonerAddressUrl(prisonNumber, addressEntityPrimaryNotMail.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = OK,
        ).returnResult().responseBody!!
        assertThat(responsePrimaryNotMail.isPrimary).isTrue()
        assertThat(responsePrimaryNotMail.isMail).isFalse()

        val addressEntityNotPrimaryMail = addressRepository.saveAndFlush(addressEntity(AddressStatusCode.MA))
        val responseNotPrimaryMail = sendGetRequestAsserted<PrisonAddress>(
          url = getPrisonerAddressUrl(prisonNumber, addressEntityNotPrimaryMail.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = OK,
        ).returnResult().responseBody!!
        assertThat(responseNotPrimaryMail.isPrimary).isFalse()
        assertThat(responseNotPrimaryMail.isMail).isTrue()

        val addressEntityNotPrimaryNotMail = addressRepository.saveAndFlush(addressEntity(AddressStatusCode.P))
        val responseNotPrimaryNotMail = sendGetRequestAsserted<PrisonAddress>(
          url = getPrisonerAddressUrl(prisonNumber, addressEntityNotPrimaryNotMail.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = OK,
        ).returnResult().responseBody!!
        assertThat(responseNotPrimaryNotMail.isPrimary).isFalse()
        assertThat(responseNotPrimaryNotMail.isMail).isFalse()
      }
    }
  }

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
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
          url = updatePrisonerAddressUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerAddressUrl(prisonNumber, addressId),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Address not found")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<String>(
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

    @Nested
    inner class Updating {

      @Test
      fun `successful update of an address`() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val existingAddress = addressRepository.saveAndFlush(
          AddressEntity(
            postcode = randomPostcode(),
            person = personEntity,
          ),
        )

        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUrl(prisonNumber, existingAddress.updateId.toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )

        val updatedAddress = addressRepository.findByUpdateId(existingAddress.updateId!!)!!
        assertAddressMatches(validRequestBody, updatedAddress)
      }

      @Test
      fun `successful update of an address does not delete its original contacts or address usages`() {
        stubPersonMatchUpsert()
        stubPersonMatchScores()
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val existingAddress = addressRepository.saveAndFlush(
          AddressEntity(
            postcode = randomPostcode(),
            person = personEntity,
          ),
        )
        val existingAddressUsage = addressUsageRepository.saveAndFlush(
          AddressUsageEntity(
            address = existingAddress,
            usageCode = AddressUsageCode.HOME,
            active = true,
          ),
        )
        val existingAddressContact = contactRepository.saveAndFlush(ContactEntity(address = existingAddress, contactType = HOME))

        sendPutRequestAsserted<Unit>(
          url = updatePrisonerAddressUrl(prisonNumber, existingAddress.updateId.toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )

        val updatedAddress = addressRepository.findByUpdateId(existingAddress.updateId!!)!!
        val retrievedAddressUsage = updatedAddress.usages.single()
        assertThat(retrievedAddressUsage.id).isEqualTo(existingAddressUsage.id)
        assertThat(retrievedAddressUsage.updateId).isEqualTo(existingAddressUsage.updateId)

        val retrievedContact = updatedAddress.contacts.single()
        assertThat(retrievedContact.id).isEqualTo(existingAddressContact.id)
        assertThat(retrievedContact.updateId).isEqualTo(existingAddressContact.updateId)
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

  private fun getPrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
  private fun createPrisonerAddressUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/address"
  private fun updatePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
  private fun deletePrisonerAddressUrl(prisonNumber: String, addressId: String) = "/syscon-sync/person/$prisonNumber/address/$addressId"
}
