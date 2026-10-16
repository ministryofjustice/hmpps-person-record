package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContactType
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonContactResponse
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.BUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime
import java.util.UUID

class PrisonAPIContactsControllerIntTest(
  @Autowired private val contactRepository: ContactRepository,
) : WebTestBase() {

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @DisplayName("GET /person/prison/{prisonNumber}/contacts")
  inner class GetContactsByPrisonNumber {

    private val prisonNumber = randomPrisonNumber()

    @BeforeAll
    fun beforeAll() {
      createPersonWithNewKey(
        createRandomPrisonPersonDetails(prisonNumber).copy(
          contacts = listOf(
            Contact(contactType = HOME, contactValue = "01234 111111", createUserId = "CREATOR"),
            Contact(contactType = MOBILE, contactValue = "07777 222222", extension = "12"),
            Contact(contactType = EMAIL, contactValue = "foo@bar.example"),
          ),
        ),
      )
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return all contacts when no filters supplied`() {
        val contacts = getContacts(contactsUrl(prisonNumber))

        assertThat(contacts.map { it.type.code }).containsExactlyInAnyOrder(HOME.name, MOBILE.name, EMAIL.name)
        val home = contacts.single { it.type.code == HOME.name }
        assertThat(home.value).isEqualTo("01234 111111")
        assertThat(home.type.description).isEqualTo(HOME.description)
        assertThat(home.createUserId).isEqualTo("CREATOR")
        assertThat(home.contactId).isNotBlank()
        assertThat(contacts.single { it.type.code == MOBILE.name }.extension).isEqualTo("12")
      }

      @Test
      fun `should only return included types`() {
        val contacts = getContacts("${contactsUrl(prisonNumber)}?includeTypes=HOME,MOBILE")

        assertThat(contacts.map { it.type.code }).containsExactlyInAnyOrder(HOME.name, MOBILE.name)
      }

      @Test
      fun `should not return excluded types`() {
        val contacts = getContacts("${contactsUrl(prisonNumber)}?excludeTypes=EMAIL")

        assertThat(contacts.map { it.type.code }).containsExactlyInAnyOrder(HOME.name, MOBILE.name)
      }

      @Test
      fun `should apply include and exclude filters together`() {
        val contacts = getContacts("${contactsUrl(prisonNumber)}?includeTypes=HOME,EMAIL&excludeTypes=EMAIL")

        assertThat(contacts.map { it.type.code }).containsExactly(HOME.name)
      }

      private fun getContacts(url: String) = sendGetRequestAsserted<List<PrisonContactResponse>>(
        url = url,
        roles = listOf(API_READ_ONLY),
        expectedStatus = OK,
      ).returnResult().responseBody!!
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when the prison number does not exist`() {
        sendGetRequestAsserted<String>(
          url = contactsUrl(randomPrisonNumber()),
          roles = listOf(API_READ_ONLY),
          expectedStatus = NOT_FOUND,
        )
      }

      @Test
      fun `should return bad request when include type is invalid`() {
        sendGetRequestAsserted<String>(
          url = "${contactsUrl(prisonNumber)}?includeTypes=NOT_A_TYPE",
          roles = listOf(API_READ_ONLY),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when exclude type is invalid`() {
        sendGetRequestAsserted<String>(
          url = "${contactsUrl(prisonNumber)}?excludeTypes=NOT_A_TYPE",
          roles = listOf(API_READ_ONLY),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendGetRequestAsserted<String>(
          url = contactsUrl(prisonNumber),
          roles = listOf("UNSUPPORTED-ROLE"),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendGetRequestAsserted<Unit>(
          url = contactsUrl(prisonNumber),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("POST /person/prison/{prisonNumber}/contacts")
  inner class CreateContactByPrisonNumber {

    @Nested
    inner class HappyPath {
      @Test
      fun `should create the contact and return it`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber).copy(contacts = emptyList()))
        val request = validContactRequest()

        val response = sendPostRequestAsserted<PrisonContactResponse>(
          url = contactsUrl(prisonNumber),
          body = request,
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = CREATED,
        ).returnResult().responseBody!!

        assertThat(response.type).isEqualTo(CanonicalContactType.from(BUS))
        assertThat(response.value).isEqualTo(request.value)
        assertThat(response.extension).isEqualTo(request.extension)
        assertThat(response.createUserId).isEqualTo(request.userId)
        assertThat(response.createDateTime).isNotNull()

        val contactEntity = personRepository.findByPrisonNumber(prisonNumber)!!.contacts.single()
        assertThat(contactEntity.updateId.toString()).isEqualTo(response.contactId)
        assertThat(contactEntity.contactType).isEqualTo(BUS)
        assertThat(contactEntity.contactValue).isEqualTo(request.value)
        assertThat(contactEntity.extension).isEqualTo(request.extension)
        assertThat(contactEntity.createUserId).isEqualTo(request.userId)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when the prison number does not exist`() {
        val prisonNumber = randomPrisonNumber()
        val response = sendPostRequestAsserted<String>(
          url = contactsUrl(prisonNumber),
          body = validContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Person with $prisonNumber not found")
      }

      @Test
      fun `should return bad request when value is missing`() {
        sendPostRequestAsserted<String>(
          url = contactsUrl(randomPrisonNumber()),
          body = invalidContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }

      @Test
      fun `should return bad request when contact type code is invalid`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        sendPostRequestAsserted<String>(
          url = contactsUrl(prisonNumber),
          body = mapOf("type" to "NOT_A_TYPE", "value" to "01234 567 890", "userId" to "TEST"),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPostRequestAsserted<String>(
          url = contactsUrl(randomPrisonNumber()),
          body = validContactRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPostRequestAsserted<Unit>(
          url = contactsUrl(randomPrisonNumber()),
          body = validContactRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  @Nested
  @DisplayName("PUT /person/prison/{prisonNumber}/contacts/{contactId}")
  inner class UpdateContactByPrisonNumber {

    @Nested
    inner class HappyPath {
      @Test
      fun `should update the contact and return it`() {
        val prisonNumber = randomPrisonNumber()
        val createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0)
        createPersonWithNewKey(
          createRandomPrisonPersonDetails(prisonNumber).copy(
            contacts = listOf(Contact(contactType = HOME, contactValue = "01234 111111", createDateTime = createDateTime, createUserId = "CREATOR")),
          ),
        )
        val contactId = personRepository.findByPrisonNumber(prisonNumber)!!.contacts.single().updateId.toString()
        val request = validContactRequest()

        val response = sendPutRequestAsserted<PrisonContactResponse>(
          url = contactUrl(prisonNumber, contactId),
          body = request,
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = OK,
        ).returnResult().responseBody!!

        assertThat(response.contactId).isEqualTo(contactId)
        assertThat(response.type).isEqualTo(CanonicalContactType.from(BUS))
        assertThat(response.value).isEqualTo(request.value)
        assertThat(response.extension).isEqualTo(request.extension)
        assertThat(response.createUserId).isEqualTo("CREATOR")
        assertThat(response.modifyUserId).isEqualTo(request.userId)
        assertThat(response.modifyDateTime).isNotNull()

        val contactEntity = contactRepository.findByUpdateId(UUID.fromString(contactId))!!
        assertThat(contactEntity.contactType).isEqualTo(BUS)
        assertThat(contactEntity.contactValue).isEqualTo(request.value)
        assertThat(contactEntity.extension).isEqualTo(request.extension)
        assertThat(contactEntity.createDateTime).isEqualTo(createDateTime)
        assertThat(contactEntity.createUserId).isEqualTo("CREATOR")
        assertThat(contactEntity.modifyUserId).isEqualTo(request.userId)
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found when the prison number does not exist`() {
        sendPutRequestAsserted<String>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_FOUND,
        )
      }

      @Test
      fun `should return not found when the contact does not exist for the person`() {
        val prisonNumber = randomPrisonNumber()
        createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber))
        val contactId = UUID.randomUUID().toString()
        val response = sendPutRequestAsserted<String>(
          url = contactUrl(prisonNumber, contactId),
          body = validContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = NOT_FOUND,
        ).returnResult().responseBody!!
        assertThat(response).contains("Contact with $contactId not found for person with $prisonNumber")
      }

      @Test
      fun `should return bad request when value is missing`() {
        sendPutRequestAsserted<String>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = invalidContactRequest(),
          roles = listOf(PRISON_API_READ_WRITE),
          expectedStatus = BAD_REQUEST,
        )
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return Access Denied 403 when role is wrong`() {
        sendPutRequestAsserted<String>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validContactRequest(),
          roles = listOf(API_READ_ONLY),
          expectedStatus = FORBIDDEN,
        )
      }

      @Test
      fun `should return UNAUTHORIZED 401 when role is not set`() {
        sendPutRequestAsserted<Unit>(
          url = contactUrl(randomPrisonNumber(), UUID.randomUUID().toString()),
          body = validContactRequest(),
          roles = emptyList(),
          expectedStatus = UNAUTHORIZED,
          sendAuthorised = false,
        )
      }
    }
  }

  private fun contactsUrl(prisonNumber: String) = "/person/prison/$prisonNumber/contacts"
  private fun contactUrl(prisonNumber: String, contactId: String) = "/person/prison/$prisonNumber/contacts/$contactId"

  private fun validContactRequest() = PrisonContactRequest(
    type = BUS,
    value = "01234 567 890",
    extension = "123",
    userId = "TEST",
  )

  private fun invalidContactRequest() = mapOf(
    "type" to BUS,
    "userId" to "TEST",
  )
}
