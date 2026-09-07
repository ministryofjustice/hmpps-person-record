package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDate

class PersonServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var personService: PersonService

  @Autowired
  lateinit var pseudonymRepository: PseudonymRepository

  @MockitoSpyBean
  private lateinit var mockReclusterService: ReclusterService

  @MockitoSpyBean
  private lateinit var mockPersonMatchService: PersonMatchService

  @Nested
  inner class ProcessPersonAliases {

    @BeforeEach
    fun setup() {
      stubPersonMatchUpsert()
    }

    @Test
    fun `successful save deletes orphaned pseudonyms`() {
      val prisonNumber = randomPrisonNumber()
      val personEntity = createPerson(
        createRandomPrisonPersonDetails(prisonNumber).copy(
          firstName = "aliasFirst1ToBeDeleted",
          lastName = "aliasLast1ToBeDeleted",
          dateOfBirth = LocalDate.of(1980, 1, 1),
          aliases = listOf(
            Alias(firstName = "aliasFirst2ToBeDeleted", lastName = "aliasLast2ToBeDeleted", dateOfBirth = LocalDate.of(1980, 1, 1)),
          ),
        ),
      )
      // Assert that the pseudonyms exist before we process the person with new aliases
      val pseudonymsToBeDeleted = personRepository.findByPrisonNumber(prisonNumber)!!.pseudonyms
      assertThat(pseudonymsToBeDeleted).hasSize(2)
      assertThat(pseudonymsToBeDeleted[0].id).isNotNull()
      assertThat(pseudonymsToBeDeleted[1].id).isNotNull()

      val personWithNewAliases = Person.from(personEntity).copy(
        firstName = "aliasFirst1New",
        lastName = "aliasLast1New",
        dateOfBirth = LocalDate.of(1980, 1, 1),
        aliases = listOf(
          Alias(firstName = "aliasFirst2New", lastName = "aliasLast2New", dateOfBirth = LocalDate.of(1980, 1, 1)),
        ),
      )

      personService.processPerson(personWithNewAliases) { personEntity }

      val orphanedPseudonyms = pseudonymsToBeDeleted.map { pseudonymRepository.findById(it.id!!) }
      assertThat(orphanedPseudonyms).allMatch { it.isEmpty }
    }

    @Test
    fun `should reuse existing pseudonyms where they match and create new where they do not`() {
      val prisonNumber = randomPrisonNumber()
      val person = createRandomPrisonPersonDetails(prisonNumber).copy(
        firstName = "aliasToBeReusedFirst",
        lastName = "aliasToBeReusedLast",
        dateOfBirth = LocalDate.of(1980, 1, 1),
        aliases = listOf(
          Alias(firstName = "aliasToBeDiscardedFirst", lastName = "aliasToBeDiscardedLast", dateOfBirth = LocalDate.of(1980, 1, 1)),
        ),
      )
      val personEntity = createPerson(person)
      // Find the pseudonyms before we process so we can check that they are reused or deleted as appropriate
      val pseudonymToBeReused = personEntity.pseudonyms.first { it.firstName == "aliasToBeReusedFirst" }
      val pseudonymToBeDiscarded = personEntity.pseudonyms.first { it.firstName == "aliasToBeDiscardedFirst" }

      val personWithAnAliasToReuse = Person.from(personEntity).copy(
        firstName = "aliasToBeReusedFirst",
        lastName = "aliasToBeReusedLast",
        dateOfBirth = LocalDate.of(1980, 1, 1),
        aliases = listOf(
          Alias(firstName = "newAliasFirst", lastName = "newAliasLast", dateOfBirth = LocalDate.of(1980, 1, 1)),
        ),
      )

      personService.processPerson(personWithAnAliasToReuse) { personEntity }

      val updated = personRepository.findByPrisonNumber(prisonNumber)!!

      assertThat(updated.pseudonyms).anyMatch { it.id == pseudonymToBeReused.id }
      assertThat(updated.pseudonyms).noneMatch { it.id == pseudonymToBeDiscarded.id }
    }

    @Test
    fun `should retain multiple semantically identical pseudonyms`() {
      val prisonNumber = randomPrisonNumber()
      val person = createRandomPrisonPersonDetails(prisonNumber).copy(
        firstName = "identicalFirst",
        lastName = "identicalLast",
        dateOfBirth = LocalDate.of(1980, 1, 1),
        aliases = listOf(
          Alias(firstName = "identicalFirst", lastName = "identicalLast", dateOfBirth = LocalDate.of(1980, 1, 1)),
        ),
      )
      val personEntity = createPerson(person)
      // Find the pseudonyms before we process so we can check that we do indeed have two semantically identical pseudonyms with different ids
      assertThat(personEntity.pseudonyms).hasSize(2)
      val sematicallyIdenticalPseudonym1 = personEntity.pseudonyms[0]
      val sematicallyIdenticalPseudonym2 = personEntity.pseudonyms[1]
      assertThat(sematicallyIdenticalPseudonym1.id).isNotEqualTo(sematicallyIdenticalPseudonym2.id)

      personService.processPerson(
        person.copy(
          firstName = "identicalFirst",
          lastName = "identicalLast",
          dateOfBirth = LocalDate.of(1980, 1, 1),
          aliases = listOf(
            Alias(firstName = "identicalFirst", lastName = "identicalLast", dateOfBirth = LocalDate.of(1980, 1, 1)),
            Alias(firstName = "newFirst", lastName = "newLast", dateOfBirth = LocalDate.of(1980, 1, 1)), // Useful to check we've actually done something.
          ),
        ),
      ) { personEntity }

      val updated = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(updated.pseudonyms).hasSize(3)
      assertThat(updated.pseudonyms).anyMatch { it.id == sematicallyIdenticalPseudonym1.id }
      assertThat(updated.pseudonyms).anyMatch { it.id == sematicallyIdenticalPseudonym2.id }
      assertThat(updated.pseudonyms).anyMatch { it.firstName == "newFirst" }
    }

    @Test
    fun `should swap when pseudonyms change to from primary`() {
      val prisonNumber = randomPrisonNumber()
      val person = createRandomPrisonPersonDetails(prisonNumber).copy(
        firstName = "primaryFirst",
        lastName = "primaryLast",
        dateOfBirth = LocalDate.of(1980, 1, 1),
        aliases = listOf(
          Alias(firstName = "secondaryFirst", lastName = "secondaryLast", dateOfBirth = LocalDate.of(1980, 1, 1)),
        ),
      )
      val personEntity = createPerson(person)
      val primaryPseudonym = personEntity.pseudonyms[0]
      val secondaryPseudonym = personEntity.pseudonyms[1]
      assertThat(primaryPseudonym.nameType).isEqualTo(NameType.PRIMARY)
      assertThat(secondaryPseudonym.nameType).isEqualTo(NameType.ALIAS)

      personService.processPerson(
        person.copy(
          firstName = "secondaryFirst",
          lastName = "secondaryLast",
          dateOfBirth = LocalDate.of(1980, 1, 1),
          aliases = listOf(
            Alias(firstName = "primaryFirst", lastName = "primaryLast", dateOfBirth = LocalDate.of(1980, 1, 1)),
          ),
        ),
      ) { personEntity }

      val updated = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(updated.pseudonyms).hasSize(2)
      assertThat(updated.pseudonyms).anyMatch { it.id == primaryPseudonym.id && it.nameType == NameType.ALIAS } // Should now be an alias
      assertThat(updated.pseudonyms).anyMatch { it.id == secondaryPseudonym.id && it.nameType == NameType.PRIMARY } // Should now be primary
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
