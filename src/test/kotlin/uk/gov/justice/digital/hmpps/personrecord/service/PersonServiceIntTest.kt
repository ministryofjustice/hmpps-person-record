package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.MOBILE
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PersonServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var personService: PersonService

  @MockitoSpyBean
  private lateinit var mockReclusterService: ReclusterService

  @MockitoSpyBean
  private lateinit var mockPersonMatchService: PersonMatchService

  @Nested
  inner class AddressContactsIgnore {

    @Test
    fun `should not set contacts when specified in creation`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val personToCreate = createRandomPrisonPersonDetails(randomPrisonNumber()).copy(contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")))
      assertThat(personToCreate.contacts).isNotEmpty()
      val created = personService.processPerson(personToCreate, setOf(ContactEntity::class)) { null }
      assertThat(created.contacts).isEmpty()
    }

    @Test
    fun `should not set contacts when specified in update`() {
      val personWithNoContact = createRandomPrisonPersonDetails(randomPrisonNumber())
      val created = createPerson(personWithNoContact)
      assertThat(created.contacts).isEmpty()
      val personWithContact = personWithNoContact.copy(contacts = listOf(Contact(MOBILE, randomPhoneNumber(), "+44")))
      val updated = personService.processPerson(personWithContact, setOf(ContactEntity::class)) { created }
      assertThat(updated.contacts).isEmpty()
    }

    @Test
    fun `should not set addresses when specified in creation`() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      val personToCreate = createRandomPrisonPersonDetails(randomPrisonNumber())
      assertThat(personToCreate.addresses).isNotEmpty()
      val created = personService.processPerson(personToCreate, setOf(AddressEntity::class)) { null }
      assertThat(created.addresses).isEmpty()
    }

    @Test
    fun `should not set addresses when specified in update`() {
      val personNoAddress = createRandomPrisonPersonDetails(randomPrisonNumber()).copy(addresses = emptyList())
      val created = createPerson(personNoAddress)
      assertThat(created.addresses).isEmpty()
      val personWithAddress = personNoAddress.copy(
        addresses = listOf(
          Address(postcode = randomPostcode(), fullAddress = randomFullAddress(), noFixedAbode = randomBoolean()),
        ),
      )
      val updated = personService.processPerson(personWithAddress, setOf(AddressEntity::class)) { created }
      assertThat(updated.addresses).isEmpty()
    }
  }

  @Test
  fun `should log record update when change in matching fields`() {
    val cId = randomCId()
    val firstName = randomName()
    val lastName = randomName()
    val person = Person.from(LibraHearingEvent(name = Name(firstName = firstName, lastName = lastName), cId = cId))
    val existingPerson = createPersonWithNewKey(person)

    val updatedPerson = Person.from(LibraHearingEvent(name = Name(firstName = randomName(), lastName = lastName), cId = cId))
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    personService.processPerson(updatedPerson) { existingPerson }

    checkEventLogExist(existingPerson.cId!!, CPRLogEvents.CPR_RECORD_UPDATED)
  }

  @Test
  fun `should not log record update when no change in matching fields but different order`() {
    val person = createRandomProbationPersonDetails(randomCrn())
    val existingPersonEntity = createPersonWithNewKey(person)

    val updatedPerson = person.copy(references = person.references.reversed())

    personService.processPerson(updatedPerson) { existingPersonEntity }
    checkEventLog(existingPersonEntity.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { logEvents ->
      assertThat(logEvents).isEmpty()
    }
  }

  @Test
  fun `should not save to person match or recluster passive records on update`() {
    val prisonNumber = randomPrisonNumber()
    val originalEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber)) { markAsPassive() }
    val originalCluster = originalEntity.personKey

    val updatedPerson = Person.from(originalEntity).copy(firstName = randomName())

    personService.processPerson(updatedPerson) { originalEntity }

    awaitAssert {
      val updatedPerson = personRepository.findByPrisonNumber(prisonNumber)
      assertThat(updatedPerson!!.personKey!!.personUUID).isEqualTo(originalCluster!!.personUUID)
      verifyNoInteractions(mockReclusterService, mockPersonMatchService)
      checkEventLogExist(updatedPerson.prisonNumber!!, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }
}
