package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressUsagesAPIControllerIntTest.Companion.assertAddressUsageMatches
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.Companion.validPrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncAddressesHandler.Companion.toPrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncAddressesHandler.Companion.toPrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconSyncContactsHandler.Companion.toDto
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddressesAndContactsRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode.A01
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode.A02
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.BUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.HOME
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import kotlin.jvm.optionals.getOrNull

class SysconSyncPrisonMergeAPIControllerIntTest : WebTestBase() {

  @Autowired
  lateinit var contactRepository: ContactRepository

  @Autowired
  lateinit var addressUsageRepository: AddressUsageRepository

  @Nested
  inner class Merging {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event returns mappings`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      val response = prisonMergeEventAndResponseSetup(targetPrisonNumber = targetPrisonNumber, prisonMerge = validPrisonMerge(sourcePrisonNumber)).returnResult().responseBody!!
      assertThat(response.toPrisonNumber).isEqualTo(targetPrisonNumber)
    }

    @Test
    fun `should retain some addresses and overwrite others for to person after a merge and return correct id mappings`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(
          createRandomPrisonPersonDetails(fromPrisonNumber).copy(
            addresses = listOf(
              // This address will replace an address on the to person
              Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
            ),
          ),
        )
        .addPerson(
          createRandomPrisonPersonDetails(toPrisonNumber).copy(
            addresses = listOf(
              // One address of the to person will be retained, the other replaced
              Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
              Address(postcode = randomPostcode(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString(), fullAddress = randomLowerCaseString()),
            ),
          ),
        )

      val fromPersonBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
      val fromAddressBeforeMergeToDonate = fromPersonBeforeMerge.addresses.first()

      val toPersonBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      val toAddressBeforeMergeToBeRetained = toPersonBeforeMerge.addresses.first()
      val toAddressBeforeMergeToBeReplaced = toPersonBeforeMerge.addresses.last()

      // The to person retains one of the addresses and the other is replaced by the from person address
      val mergeAddressReqBodyRetained = toAddressBeforeMergeToBeRetained.toPrisonAddress().copy(nomisAddressId = 1L)
      val mergeAddressReqBodyDonated = fromAddressBeforeMergeToDonate.toPrisonAddress().copy(nomisAddressId = 2L)
      val requestBody = PrisonMerge(
        fromPrisonNumber = fromPrisonNumber,
        toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
          addresses = listOf(mergeAddressReqBodyRetained, mergeAddressReqBodyDonated),
          contacts = emptyList(),
        ),
      )

      val actualResponseBody = prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

      // The from person has donated its address to the to person
      val fromPersonAfterMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
      assertThat(fromPersonAfterMerge.addresses.size).isEqualTo(0)

      // The to person should still have two addresses
      // One was retained. One was replaced by the from person
      val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      assertThat(toPersonAfterMerge.addresses).hasSize(2)

      val toAddressAfterMergeRetained = toPersonAfterMerge.addresses.first { it.id == toAddressBeforeMergeToBeRetained.id }
      assertThat(toAddressAfterMergeRetained.updateId).isEqualTo(toAddressBeforeMergeToBeRetained.updateId)
      assertAddressMatches(mergeAddressReqBodyRetained, toAddressAfterMergeRetained)

      val toAddressAfterMergeDonated = toPersonAfterMerge.addresses.first { it.id == fromAddressBeforeMergeToDonate.id }
      assertThat(toAddressAfterMergeDonated.updateId).isEqualTo(fromAddressBeforeMergeToDonate.updateId)
      assertAddressMatches(mergeAddressReqBodyDonated, toAddressAfterMergeDonated)

      assertNull(addressRepository.findByUpdateId(toAddressBeforeMergeToBeReplaced.updateId!!))

      val expectedBody = SysconPersonMergeResponseBody(
        toPrisonNumber = toPrisonNumber,
        sysconAddressesAndContactsResponseBody = SysconAddressesAndContactsResponseBody(
          prisonNumber = toPrisonNumber,
          addressesMappings = listOf(
            SysconAddressMapping(
              nomisAddressId = mergeAddressReqBodyRetained.nomisAddressId!!,
              cprAddressId = toAddressBeforeMergeToBeRetained.updateId.toString(),
              addressUsageMappings = emptyList(),
              contactMappings = emptyList(),
            ),
            SysconAddressMapping(
              nomisAddressId = mergeAddressReqBodyDonated.nomisAddressId!!,
              cprAddressId = fromAddressBeforeMergeToDonate.updateId.toString(),
              addressUsageMappings = emptyList(),
              contactMappings = emptyList(),
            ),
          ),
          contactMappings = emptyList(),
        ),
      )
      assertThat(actualResponseBody).isEqualTo(expectedBody)
    }

    @Test
    fun `should retain the to address with its original address usage and contact and an address usage and contact donated from the from address after a merge`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(
          createRandomPrisonPersonDetails(fromPrisonNumber).copy(
            addresses = listOf(
              // The usage and contact on this address will be donated to the to person's address
              Address(
                postcode = randomPostcode(),
                createDateTime = randomDateTime(),
                createUserId = randomLowerCaseString(),
                fullAddress = randomLowerCaseString(),
                usages = listOf(AddressUsage(addressUsageCode = A02, isActive = true, createDateTime = randomDateTime(), createUserId = randomLowerCaseString())),
                contacts = listOf(Contact(contactType = BUS, contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString())),
              ),
            ),
          ),
        )
        .addPerson(
          createRandomPrisonPersonDetails(toPrisonNumber).copy(
            addresses = listOf(
              // This address and its usage and contact will be retained
              Address(
                postcode = randomPostcode(),
                createDateTime = randomDateTime(),
                createUserId = randomLowerCaseString(),
                fullAddress = randomLowerCaseString(),
                usages = listOf(AddressUsage(addressUsageCode = A01, isActive = true, createDateTime = randomDateTime(), createUserId = randomLowerCaseString())),
                contacts = listOf(Contact(contactType = HOME, contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString())),
              ),
            ),
          ),
        )

      val fromAddressBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!.addresses.first()
      val fromAddressUsageBeforeMergeToDonate = fromAddressBeforeMerge.usages.first()
      val fromAddressContactBeforeMergeToDonate = fromAddressBeforeMerge.contacts.first()

      val toAddressBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.addresses.first()
      val toAddressUsageBeforeMergeToBeRetained = toAddressBeforeMerge.usages.first()
      val toAddressContactBeforeMergeToBeRetained = toAddressBeforeMerge.contacts.first()

      val nomisAddressId = 1L // The address usage id has to be the same as the address id it is associated with.
      val toAddressContactToBeRetainedReqBody = toAddressContactBeforeMergeToBeRetained.toDto().copy(nomisContactId = 2L)
      val fromAddressContactToDonateReqBody = fromAddressContactBeforeMergeToDonate.toDto().copy(nomisContactId = 3L)
      val toAddressUsageToBeRetainedReqBody = toAddressUsageBeforeMergeToBeRetained.toPrisonAddressUsage().copy(nomisAddressUsageId = nomisAddressId)
      val fromAddressUsageToDonateReqBody = fromAddressUsageBeforeMergeToDonate.toPrisonAddressUsage().copy(nomisAddressUsageId = nomisAddressId)
      // The to address keeps its own usage and contact and gains the usage and contact from the from address
      val mergeAddressReqBody = toAddressBeforeMerge.toPrisonAddress()
        .copy(
          nomisAddressId = 1L,
          contacts = listOf(toAddressContactToBeRetainedReqBody, fromAddressContactToDonateReqBody),
          addressUsage = listOf(toAddressUsageToBeRetainedReqBody, fromAddressUsageToDonateReqBody),
        )
      val requestBody = PrisonMerge(
        fromPrisonNumber = fromPrisonNumber,
        toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
          addresses = listOf(mergeAddressReqBody),
          contacts = emptyList(),
        ),
      )

      prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

      // The to person still has its original address
      val toAddressAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.addresses.first { it.id == toAddressBeforeMerge.id }
      assertThat(toAddressAfterMerge.updateId).isEqualTo(toAddressBeforeMerge.updateId)
      assertAddressMatches(mergeAddressReqBody, toAddressAfterMerge)

      // The to address has both its original usage and the donated usage
      assertThat(toAddressAfterMerge.usages).hasSize(2)
      val toAddressUsageAfterMergeRetained = toAddressAfterMerge.usages.first { it.id == toAddressUsageBeforeMergeToBeRetained.id }
      assertThat(toAddressUsageAfterMergeRetained.updateId).isEqualTo(toAddressUsageBeforeMergeToBeRetained.updateId)
      assertAddressUsageMatches(toAddressUsageToBeRetainedReqBody, toAddressUsageAfterMergeRetained)

      val toAddressUsageAfterMergeDonated = toAddressAfterMerge.usages.first { it.id == fromAddressUsageBeforeMergeToDonate.id }
      assertThat(toAddressUsageAfterMergeDonated.updateId).isEqualTo(fromAddressUsageBeforeMergeToDonate.updateId)
      assertAddressUsageMatches(fromAddressUsageToDonateReqBody, toAddressUsageAfterMergeDonated)

      // The to address has both its original contact and the donated contact
      assertThat(toAddressAfterMerge.contacts).hasSize(2)
      val toAddressContactAfterMergeRetained = toAddressAfterMerge.contacts.first { it.id == toAddressContactBeforeMergeToBeRetained.id }
      assertThat(toAddressContactAfterMergeRetained.updateId).isEqualTo(toAddressContactBeforeMergeToBeRetained.updateId)
      assertContactMatches(toAddressContactToBeRetainedReqBody, toAddressContactAfterMergeRetained)

      val toAddressContactAfterMergeDonated = toAddressAfterMerge.contacts.first { it.id == fromAddressContactBeforeMergeToDonate.id }
      assertThat(toAddressContactAfterMergeDonated.updateId).isEqualTo(fromAddressContactBeforeMergeToDonate.updateId)
      assertContactMatches(fromAddressContactToDonateReqBody, toAddressContactAfterMergeDonated)
    }

    @Test
    fun `should retain the to address with its original address usage and add a new address usage after a merge`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(fromPrisonNumber))
        .addPerson(
          createRandomPrisonPersonDetails(toPrisonNumber).copy(
            addresses = listOf(
              // This address and its usage will be retained
              Address(
                postcode = randomPostcode(),
                createDateTime = randomDateTime(),
                createUserId = randomLowerCaseString(),
                fullAddress = randomLowerCaseString(),
                usages = listOf(AddressUsage(addressUsageCode = A01, isActive = true, createDateTime = randomDateTime(), createUserId = randomLowerCaseString())),
              ),
            ),
          ),
        )

      val toAddressBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.addresses.first()
      val toAddressUsageBeforeMergeToBeRetained = toAddressBeforeMerge.usages.first()

      val nomisAddressId = 1L // The address usage id has to be the same as the address id it is associated with.
      val toAddressUsageToBeRetainedReqBody = toAddressUsageBeforeMergeToBeRetained.toPrisonAddressUsage().copy(nomisAddressUsageId = nomisAddressId)
      // A brand new usage with no cpr id
      val newAddressUsageReqBody = PrisonAddressUsage(
        nomisAddressUsageId = nomisAddressId,
        addressUsageCode = A02,
        isActive = false,
        createDateTime = randomDateTime(),
        createUserId = randomLowerCaseString(),
      )
      val mergeAddressReqBody = toAddressBeforeMerge.toPrisonAddress().copy(
        nomisAddressId = nomisAddressId,
        addressUsage = listOf(toAddressUsageToBeRetainedReqBody, newAddressUsageReqBody),
      )
      val requestBody = PrisonMerge(
        fromPrisonNumber = fromPrisonNumber,
        toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
          addresses = listOf(mergeAddressReqBody),
          contacts = emptyList(),
        ),
      )

      val actualResponseBody = prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

      // The to person still has its original address
      val toAddressAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.addresses.single()
      assertThat(toAddressAfterMerge.updateId).isEqualTo(toAddressBeforeMerge.updateId)
      assertAddressMatches(mergeAddressReqBody, toAddressAfterMerge)

      // The to address has its original usage and the new usage
      assertThat(toAddressAfterMerge.usages).hasSize(2)
      val toAddressUsageAfterMergeRetained = toAddressAfterMerge.usages.first { it.id == toAddressUsageBeforeMergeToBeRetained.id }
      assertThat(toAddressUsageAfterMergeRetained.updateId).isEqualTo(toAddressUsageBeforeMergeToBeRetained.updateId)
      assertAddressUsageMatches(toAddressUsageToBeRetainedReqBody, toAddressUsageAfterMergeRetained)

      val toAddressUsageAfterMergeNew = toAddressAfterMerge.usages.first { it.usageCode == A02 }
      assertThat(toAddressUsageAfterMergeNew.updateId).isNotNull()
      assertAddressUsageMatches(newAddressUsageReqBody, toAddressUsageAfterMergeNew)

      val expectedBody = SysconPersonMergeResponseBody(
        toPrisonNumber = toPrisonNumber,
        sysconAddressesAndContactsResponseBody = SysconAddressesAndContactsResponseBody(
          prisonNumber = toPrisonNumber,
          addressesMappings = listOf(
            SysconAddressMapping(
              nomisAddressId = nomisAddressId,
              cprAddressId = toAddressBeforeMerge.updateId.toString(),
              addressUsageMappings = listOf(
                SysconAddressUsageMapping(
                  nomisAddressUsageId = nomisAddressId,
                  nomisAddressUsageCode = A01,
                  cprAddressUsageId = toAddressUsageBeforeMergeToBeRetained.updateId.toString(),
                ),
                SysconAddressUsageMapping(
                  nomisAddressUsageId = nomisAddressId,
                  nomisAddressUsageCode = A02,
                  cprAddressUsageId = toAddressUsageAfterMergeNew.updateId.toString(),
                ),
              ),
              contactMappings = emptyList(),
            ),
          ),
          contactMappings = emptyList(),
        ),
      )
      assertThat(actualResponseBody).isEqualTo(expectedBody)
    }

    @Test
    fun `should move primary and mail flags from the retained to address to the donated from address after a merge`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(
          createRandomPrisonPersonDetails(fromPrisonNumber).copy(
            addresses = listOf(
              // This primary and mail address will be donated and remain primary and mail
              Address(
                postcode = randomPostcode(),
                createDateTime = randomDateTime(),
                createUserId = randomLowerCaseString(),
                fullAddress = randomLowerCaseString(),
                statusCode = AddressStatusCode.PM,
              ),
            ),
          ),
        )
        .addPerson(
          createRandomPrisonPersonDetails(toPrisonNumber).copy(
            addresses = listOf(
              // This primary and mail address will be retained but will no longer be primary or mail
              Address(
                postcode = randomPostcode(),
                createDateTime = randomDateTime(),
                createUserId = randomLowerCaseString(),
                fullAddress = randomLowerCaseString(),
                statusCode = AddressStatusCode.PM,
              ),
            ),
          ),
        )

      val fromAddressBeforeMergeToDonate = personRepository.findByPrisonNumber(fromPrisonNumber)!!.addresses.first()
      assertThat(fromAddressBeforeMergeToDonate.statusCode).isEqualTo(AddressStatusCode.PM)
      val toAddressBeforeMergeToBeRetained = personRepository.findByPrisonNumber(toPrisonNumber)!!.addresses.first()
      assertThat(toAddressBeforeMergeToBeRetained.statusCode).isEqualTo(AddressStatusCode.PM)

      // Only one address can be primary and only one can be mail, so the to address loses both flags
      val mergeAddressReqBodyRetained = toAddressBeforeMergeToBeRetained.toPrisonAddress().copy(nomisAddressId = 1L, isPrimary = false, isMail = false)
      val mergeAddressReqBodyDonated = fromAddressBeforeMergeToDonate.toPrisonAddress().copy(nomisAddressId = 2L, isPrimary = true, isMail = true)
      val requestBody = PrisonMerge(
        fromPrisonNumber = fromPrisonNumber,
        toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
          addresses = listOf(mergeAddressReqBodyRetained, mergeAddressReqBodyDonated),
          contacts = emptyList(),
        ),
      )

      prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

      val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      assertThat(toPersonAfterMerge.addresses).hasSize(2)

      // The retained to address is no longer primary or mail
      val toAddressAfterMergeRetained = toPersonAfterMerge.addresses.first { it.id == toAddressBeforeMergeToBeRetained.id }
      assertThat(toAddressAfterMergeRetained.updateId).isEqualTo(toAddressBeforeMergeToBeRetained.updateId)
      assertThat(toAddressAfterMergeRetained.statusCode).isNull()

      // The donated from address is now the primary and mail address
      val toAddressAfterMergeDonated = toPersonAfterMerge.addresses.first { it.id == fromAddressBeforeMergeToDonate.id }
      assertThat(toAddressAfterMergeDonated.updateId).isEqualTo(fromAddressBeforeMergeToDonate.updateId)
      assertThat(toAddressAfterMergeDonated.statusCode).isEqualTo(AddressStatusCode.PM)
    }

    @Test
    fun `should retain some contacts and overwrite others for to person after a merge and return correct id mappings`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(
          createRandomPrisonPersonDetails(fromPrisonNumber).copy(
            contacts = listOf(
              // This contact will replace a contact on the to person
              Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
            ),
          ),
        )
        .addPerson(
          createRandomPrisonPersonDetails(toPrisonNumber).copy(
            contacts = listOf(
              // One contact of the to person will be retained, the other replaced
              Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
              Contact(contactType = randomContactType(), contactValue = randomLowerCaseString(), createDateTime = randomDateTime(), createUserId = randomLowerCaseString()),
            ),
          ),
        )

      val fromPersonBeforeMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
      val fromContactBeforeMergeToDonate = fromPersonBeforeMerge.contacts.first()
      val toPersonBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      val toContactBeforeMergeToBeRetained = toPersonBeforeMerge.contacts.first()
      val toContactBeforeMergeToBeReplaced = toPersonBeforeMerge.contacts.last()

      val mergeContactReqBodyRetained = toContactBeforeMergeToBeRetained.toDto().copy(nomisContactId = 1L)
      val mergeContactReqBodyDonated = fromContactBeforeMergeToDonate.toDto().copy(nomisContactId = 2L)
      val requestBody = PrisonMerge(
        fromPrisonNumber = fromPrisonNumber,
        toPrisonAddressesAndContactsRequest = PrisonAddressesAndContactsRequest(
          contacts = listOf(mergeContactReqBodyRetained, mergeContactReqBodyDonated),
          addresses = emptyList(),
        ),
      )

      val actualResponseBody = prisonMergeEventAndResponseSetup(targetPrisonNumber = toPrisonNumber, prisonMerge = requestBody).returnResult().responseBody!!

      // The from person has donated its contact to the to person
      val fromPersonAfterMerge = personRepository.findByPrisonNumber(fromPrisonNumber)!!
      assertThat(fromPersonAfterMerge.contacts.size).isEqualTo(0)

      // The to person should still have two contacts
      // One was retained. One was replaced by the from person
      val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      assertThat(toPersonAfterMerge.contacts).hasSize(2)

      val toContactAfterMergeRetained = toPersonAfterMerge.contacts.first { it.id == toContactBeforeMergeToBeRetained.id }
      assertThat(toContactAfterMergeRetained.updateId).isEqualTo(toContactBeforeMergeToBeRetained.updateId)
      assertContactMatches(mergeContactReqBodyRetained, toContactAfterMergeRetained)

      val toContactAfterMergeDonated = toPersonAfterMerge.contacts.first { it.id == fromContactBeforeMergeToDonate.id }
      assertThat(toContactAfterMergeDonated.updateId).isEqualTo(fromContactBeforeMergeToDonate.updateId)
      assertContactMatches(mergeContactReqBodyDonated, toContactAfterMergeDonated)

      assertNull(contactRepository.findByUpdateId(toContactBeforeMergeToBeReplaced.updateId!!))

      val expectedBody = SysconPersonMergeResponseBody(
        toPrisonNumber = toPrisonNumber,
        sysconAddressesAndContactsResponseBody = SysconAddressesAndContactsResponseBody(
          prisonNumber = toPrisonNumber,
          addressesMappings = emptyList(),
          contactMappings = listOf(
            SysconContactMapping(
              nomisContactId = mergeContactReqBodyRetained.nomisContactId!!,
              nomisContactType = toContactBeforeMergeToBeRetained.contactType,
              cprContactId = toContactBeforeMergeToBeRetained.updateId.toString(),
            ),
            SysconContactMapping(
              nomisContactId = mergeContactReqBodyDonated.nomisContactId!!,
              nomisContactType = fromContactBeforeMergeToDonate.contactType,
              cprContactId = fromContactBeforeMergeToDonate.updateId.toString(),
            ),
          ),
        ),
      )
      assertThat(actualResponseBody).isEqualTo(expectedBody)
    }
  }

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event with records with same UUID is published`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      createPersonKey()
        .addPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(targetPrisonNumber = targetPrisonNumber, prisonMerge = validPrisonMerge(sourcePrisonNumber))

      awaitAssert {
        val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)!!
        val targetPerson = personRepository.findByPrisonNumber(targetPrisonNumber)!!
        sourcePerson.assertNotLinkedToCluster()
        sourcePerson.assertMergedTo(targetPerson)
        targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
        targetPerson.personKey?.assertClusterIsOfSize(1)
      }

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has multiple records`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourceCluster = createPersonKey()
        .addPerson(Person(prisonNumber = randomPrisonNumber(), sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))

      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber))
      awaitAssert {
        val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)!!
        sourcePerson.assertNotLinkedToCluster()
        sourcePerson.assertMergedTo(targetPerson)

        targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
        targetPerson.personKey?.assertClusterIsOfSize(1)

        sourceCluster.assertClusterIsOfSize(1)
        sourceCluster.assertClusterStatus(UUIDStatusType.ACTIVE)

        targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
        targetPerson.personKey?.assertClusterIsOfSize(1)
      }

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has a single record`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))
      val sourceClusterId = sourcePerson.personKey?.id

      prisonMergeEventAndResponseSetup(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber))

      sourcePerson.assertMergedTo(targetPerson)
      sourcePerson.assertNotLinkedToCluster()

      val sourceClusterPostMerge = personKeyRepository.findById(sourceClusterId!!).getOrNull()
      assertThat(sourceClusterPostMerge).isNull()

      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLog(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED) { eventLogs ->
        assertThat(eventLogs).hasSize(1)
        assertThat(eventLogs.first().recordMergedTo).isEqualTo(targetPerson.id)
        assertThat(eventLogs.first().personUUID).isEqualTo(sourcePerson.personKey!!.personUUID)
      }
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class ErrorHandling {

    @Test
    fun `should fail with a 500 error when message processing fails`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure")

      publishPrisonPersonMergedEvent<Unit>(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber), HttpStatus.INTERNAL_SERVER_ERROR)
    }
  }

  @Nested
  inner class PassiveStateRecords {

    @BeforeEach
    fun beforeEach() {
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event and target record passive state is maintained`() {
      stubPersonMatchScores()
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS))
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }

      prisonMergeEventAndResponseSetup(targetPrisonNumber, validPrisonMerge(sourcePrisonNumber))

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      assertThat(sourcePerson.isPassive()).isFalse()
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
      assertThat(targetPerson.isPassive()).isTrue()
      wiremock.verify(0, postRequestedFor(urlEqualTo("/person")))
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }

    @Test
    fun `processes prisoner merge event and source record passive state is not maintained on target record`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()

      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()

      val sourcePerson = createPersonWithNewKey(Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS)) { markAsPassive() }
      val targetPerson = createPersonWithNewKey(Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS))

      prisonMergeEventAndResponseSetup(
        targetPrisonNumber,
        validPrisonMerge(sourcePrisonNumber),
      )

      sourcePerson.assertNotLinkedToCluster()
      sourcePerson.assertMergedTo(targetPerson)
      assertThat(sourcePerson.isPassive()).isTrue()
      targetPerson.personKey?.assertClusterStatus(UUIDStatusType.ACTIVE)
      targetPerson.personKey?.assertClusterIsOfSize(1)
      assertThat(targetPerson.isPassive()).isFalse()
      wiremock.verify(1, postRequestedFor(urlEqualTo("/person")))
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))

      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "FROM_SOURCE_SYSTEM_ID" to sourcePrisonNumber,
          "TO_SOURCE_SYSTEM_ID" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkEventLogExist(sourcePrisonNumber, CPRLogEvents.CPR_RECORD_MERGED)
      checkEventLogExist(targetPrisonNumber, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }

  @Nested
  inner class Authorisation {

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = validPrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf(),
        expectedStatus = HttpStatus.UNAUTHORIZED,
        sendAuthorised = false,
      )
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      sendPostRequestAsserted<Unit>(
        url = prisonMergeEndpoint(randomPrisonNumber()),
        body = validPrisonMerge(fromPrisonNumber = randomPrisonNumber()),
        roles = listOf("UNSUPPORTED_ROLE"),
        expectedStatus = HttpStatus.FORBIDDEN,
      )
    }
  }

  private fun prisonMergeEndpoint(prisonNumber: String) = "/syscon-sync/person/$prisonNumber/merge"
  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  private fun prisonMergeEventAndResponseSetup(
    targetPrisonNumber: String,
    prisonMerge: PrisonMerge,
    scenario: String = BASE_SCENARIO,
    currentScenarioState: String = STARTED,
    nextScenarioState: String = STARTED,
    expectedStatus: HttpStatus = HttpStatus.CREATED,
  ): WebTestClient.BodySpec<SysconPersonMergeResponseBody, *> {
    stubPrisonResponse(
      ApiResponseSetup(prisonNumber = targetPrisonNumber),
      scenario,
      currentScenarioState,
      nextScenarioState,
    )
    return publishPrisonPersonMergedEvent<SysconPersonMergeResponseBody>(targetPrisonNumber, prisonMerge, expectedStatus)
  }
  private inline fun <reified T : Any> publishPrisonPersonMergedEvent(
    targetPrisonNumber: String,
    prisonMerge: PrisonMerge,
    expectedStatus: HttpStatus = HttpStatus.CREATED,
  ): WebTestClient.BodySpec<T, *> = sendPostRequestAsserted<T>(
    url = prisonMergeEndpoint(targetPrisonNumber),
    body = prisonMerge,
    roles = listOf(PERSON_RECORD_SYSCON_SYNC_WRITE),
    expectedStatus = expectedStatus,
  )

  private fun validPrisonMerge(fromPrisonNumber: String) = PrisonMerge(
    fromPrisonNumber = fromPrisonNumber,
    toPrisonAddressesAndContactsRequest = validPrisonAddressesAndContactsRequest,
  )

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

    fun assertContactMatches(request: PrisonContact, entity: ContactEntity) = with(entity) {
      assertThat(contactType).isEqualTo(request.type)
      assertThat(contactValue).isEqualTo(request.value)
      assertThat(extension).isEqualTo(request.extension)
      assertThat(modifyUserId).isEqualTo(request.modifyUserId)
      assertThat(modifyDateTime).isEqualTo(request.modifyDateTime)
      assertThat(createUserId).isEqualTo(request.createUserId)
      assertThat(createDateTime).isEqualTo(request.createDateTime)
    }
  }
}
