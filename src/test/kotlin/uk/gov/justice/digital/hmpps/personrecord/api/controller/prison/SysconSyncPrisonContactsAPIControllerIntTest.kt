package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.util.UUID

class SysconSyncPrisonContactsAPIControllerIntTest : WebTestBase() {

  @Autowired
  lateinit var contactRepository: ContactRepository

  @Nested
  inner class GetPrisonerContact {

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendGetRequestAsserted<String>(
          url = getPrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
          url = getPrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("\"error\":\"Not Found\"")
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `contact does not exist - returns 404 not found`() {
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendGetRequestAsserted<String>(
          url = getPrisonerContactUrl(prisonNumber, contactId),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Contact with $contactId not found for person with $prisonNumber")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendGetRequestAsserted<String>(
          url = getPrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<PrisonContact>(
          url = getPrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Retrieval {

      @Test
      fun `successful get of a person contact returns the correct response body`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val contactEntity = contactRepository.saveAndFlush(
          ContactEntity(
            person = personEntity,
            contactType = HOME,
            contactValue = "value",
            extension = "extension",
            createDateTime = LocalDateTime.of(2017, 3, 1, 12, 0),
            createUserId = "createUserId",
            modifyDateTime = LocalDateTime.of(2017, 3, 2, 12, 0),
            modifyUserId = "modifyUserId",
          ),
        )

        val response = sendGetRequestAsserted<PrisonContact>(
          url = getPrisonerContactUrl(prisonNumber, contactEntity.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = HttpStatus.OK,
        ).returnResult().responseBody!!

        assertContactMatches(response, contactEntity)
        assertThat(response.prisonNumber).isEqualTo(prisonNumber)
        assertThat(response.cprAddressId).isNull()
      }

      @Test
      fun `successful get of an address contact returns the correct response body`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val addressEntity = personRepository.findByPrisonNumber(prisonNumber)!!.addresses.first()
        val contactEntity = contactRepository.saveAndFlush(
          ContactEntity(
            address = addressEntity,
            contactType = HOME,
            contactValue = "value",
            extension = "extension",
            createDateTime = LocalDateTime.of(2017, 3, 1, 12, 0),
            createUserId = "createUserId",
            modifyDateTime = LocalDateTime.of(2017, 3, 2, 12, 0),
            modifyUserId = "modifyUserId",
          ),
        )

        val response = sendGetRequestAsserted<PrisonContact>(
          url = getPrisonerContactUrl(prisonNumber, contactEntity.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = HttpStatus.OK,
        ).returnResult().responseBody!!

        assertContactMatches(response, contactEntity)
        assertThat(response.cprAddressId).isEqualTo(addressEntity.updateId.toString())
        assertThat(response.prisonNumber).isNull()
      }
    }
  }

  @Nested
  inner class CreatePrisonerContact {

    private val validRequestBody = PrisonContact(
      nomisContactId = 11000L,
      value = "value",
      type = HOME,
      extension = "extension",
      createDateTime = LocalDateTime.of(2017, 3, 1, 12, 0),
      createUserId = "createUserId",
      modifyDateTime = LocalDateTime.of(2017, 3, 2, 12, 0),
      modifyUserId = "modifyUserId",
    )

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPostRequestAsserted<String>(
          url = createPrisonerContactUrl(randomPrisonNumber()),
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
          url = createPrisonerContactUrl(randomPrisonNumber()),
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
          url = createPrisonerContactUrl(prisonNumber),
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
          url = createPrisonerContactUrl(randomPrisonNumber()),
          body = validRequestBody,
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<SysconContactMapping>(
          url = createPrisonerContactUrl(randomPrisonNumber()),
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
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendPostRequestAsserted<SysconContactMapping>(
          url = createPrisonerContactUrl(prisonNumber),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = HttpStatus.CREATED,
          sendAuthorised = true,
        ).returnResult().responseBody!!

        val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        val contactEntity = personEntity.contacts.single { response.cprContactId == it.updateId.toString() }
        assertContactMatches(validRequestBody, contactEntity)
      }
    }
  }

  @Nested
  inner class UpdatePrisonerContact {

    private val validRequestBody = PrisonContact(
      nomisContactId = 11000L,
      value = "value",
      type = HOME,
      extension = "extension",
      createDateTime = LocalDateTime.of(2017, 3, 1, 12, 0),
      createUserId = "createUserId",
      modifyDateTime = LocalDateTime.of(2017, 3, 2, 12, 0),
      modifyUserId = "modifyUserId",
    )

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
      fun `contact does not exist - returns 404 not found`() {
        val prisonNumber = randomPrisonNumber()
        val contactId = UUID.randomUUID().toString()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val response = sendPutRequestAsserted<String>(
          url = updatePrisonerContactUrl(prisonNumber, contactId),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Contact with $contactId not found")
      }
    }

    @Nested
    inner class Auth {

      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<String>(
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validRequestBody,
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
      fun `successful update of a contact`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val contactEntity = contactRepository.saveAndFlush(ContactEntity(person = personEntity, contactType = MOBILE))
        sendPutRequestAsserted<Unit>(
          url = updatePrisonerContactUrl(prisonNumber, contactEntity.updateId.toString()),
          body = validRequestBody,
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )

        val updatedPersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!
        val updatedContactEntity = updatedPersonEntity.contacts.single { contactEntity.updateId == it.updateId }
        assertContactMatches(validRequestBody, updatedContactEntity)
      }
    }
  }

  @Nested
  inner class DeletePrisonerContact {

    @Nested
    @ActiveProfiles("prod")
    inner class ProductionProfile {

      @Test
      fun `should have the correct profile active`() {
        val response = sendDeleteRequestAsserted<String>(
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
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
        val response = sendDeleteRequestAsserted<String>(
          url = deletePrisonerContactUrl(prisonNumber, UUID.randomUUID().toString()),
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
        sendDeleteRequestAsserted<String>(
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        ).returnResult().responseBody!!
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerContactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }

    @Nested
    inner class Deleting {

      @Test
      fun `successful delete of a contact`() {
        val prisonNumber = randomPrisonNumber()
        val personEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val contactEntity = contactRepository.saveAndFlush(ContactEntity(person = personEntity, contactType = MOBILE))
        sendDeleteRequestAsserted<Unit>(
          url = deletePrisonerContactUrl(prisonNumber, contactEntity.updateId.toString()),
          roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
          expectedStatus = NO_CONTENT,
        )
        assertThat(contactRepository.findById(contactEntity.id!!)).isEmpty()
      }
    }
  }

  private fun assertContactMatches(request: PrisonContact, entity: ContactEntity) = with(entity) {
    assertThat(contactType).isEqualTo(request.type)
    assertThat(contactValue).isEqualTo(request.value)
    assertThat(extension).isEqualTo(request.extension)
    assertThat(modifyUserId).isEqualTo(request.modifyUserId)
    assertThat(modifyDateTime).isEqualTo(request.modifyDateTime)
    assertThat(createUserId).isEqualTo(request.createUserId)
    assertThat(createDateTime).isEqualTo(request.createDateTime)
  }

  private fun getPrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
  private fun createPrisonerContactUrl(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/contact"
  private fun updatePrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
  private fun deletePrisonerContactUrl(prisonNumber: String, contactId: String) = "/syscon-sync/person/$prisonNumber/contact/$contactId"
}
