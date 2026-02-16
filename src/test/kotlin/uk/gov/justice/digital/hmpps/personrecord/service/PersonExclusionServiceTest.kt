package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.temporal.ChronoUnit

class PersonExclusionServiceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personExclusionService: PersonExclusionService

  @MockitoSpyBean
  private lateinit var mockPersonKeyRepository: PersonKeyRepository

  @Test
  fun `cluster size greater than 1 - updates marker - removes person from cluster - attaches person to new cluster`() {
    val prisonerNumberOne = randomPrisonNumber()
    val prisonerNumberTwo = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberTwo))

    awaitAssert {
      val clusterBeforeExclusion = personKeyRepository.findByPersonUUID(originalPersonKeyEntity.personUUID)
      assertThat(clusterBeforeExclusion!!.personEntities.size).isEqualTo(2)
    }

    personExclusionService.exclude { personRepository.findByPrisonNumber(prisonerNumberTwo) }

    awaitAssert {
      // assert prisoner one (one that is not being excluded) has not changed clusters
      val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
      assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)
      assertThat(personOne.personKey!!.personEntities.size).isEqualTo(1)
      assertThat(personOne.isPassive()).isFalse()

      // assert prisoner two (one that IS being excluded) has changed clusters
      val personTwo = personRepository.findByPrisonNumber(prisonerNumberTwo)!!
      assertThat(personOne.personKey!!.personUUID).isNotEqualTo(personTwo.personKey!!.personUUID)
      assertThat(personTwo.personKey!!.personEntities.size).isEqualTo(1)
      assertThat(personTwo.isPassive()).isTrue()

      checkEventLogExist(prisonerNumberTwo, CPRLogEvents.CPR_UUID_CREATED)
    }
  }

  @Test
  fun `cluster size of 1 - updates marker`() {
    val prisonerNumberOne = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))

    personExclusionService.exclude { personRepository.findByPrisonNumber(prisonerNumberOne) }

    awaitAssert {
      val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
      assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)
      assertThat(personOne.personKey!!.personEntities.size).isEqualTo(1)
      assertThat(personOne.isPassive()).isTrue()
    }
  }

  @Test
  fun `error writing to db - maintains marker state`() {
    val prisonerNumberOne = randomPrisonNumber()
    val prisonerNumberTwo = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberTwo))

    awaitAssert {
      val clusterBeforeExclusion = personKeyRepository.findByPersonUUID(originalPersonKeyEntity.personUUID)
      assertThat(clusterBeforeExclusion!!.personEntities.size).isEqualTo(2)
    }

    doThrow(RuntimeException()).whenever(mockPersonKeyRepository).save(any())

    assertThrows(RuntimeException::class.java) { personExclusionService.exclude { personRepository.findByPrisonNumber(prisonerNumberTwo) } }

    awaitAssert {
      val clusterAfterExclusion = personKeyRepository.findByPersonUUID(originalPersonKeyEntity.personUUID)
      assertThat(clusterAfterExclusion!!.personEntities.size).isEqualTo(2)

      val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
      assertThat(personOne.isPassive()).isFalse()

      val personTwo = personRepository.findByPrisonNumber(prisonerNumberTwo)!!
      assertThat(personTwo.isPassive()).isFalse()
    }
  }

  @Test
  fun `already in passive state - no update to person record`() {
    val prisonerNumberOne = randomPrisonNumber()
    val originalPersonEntity = createPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
    originalPersonEntity.markAsPassive()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(originalPersonEntity)

    personExclusionService.exclude { personRepository.findByPrisonNumber(prisonerNumberOne) }

    awaitAssert {
      val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
      assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)
      assertThat(personOne.personKey!!.personEntities.size).isEqualTo(1)
      assertThat(personOne.isPassive()).isTrue()
      assertThat(personOne.lastModified!!.truncatedTo(ChronoUnit.MICROS)).isEqualTo(originalPersonEntity.lastModified!!.truncatedTo(ChronoUnit.MICROS))
    }
  }
}
