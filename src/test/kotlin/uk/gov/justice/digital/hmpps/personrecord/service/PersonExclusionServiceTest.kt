package uk.gov.justice.digital.hmpps.personrecord.service

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PersonExclusionServiceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personExclusionService: PersonExclusionService

  @MockitoSpyBean
  private lateinit var mockPersonKeyRepository: PersonKeyRepository

  @Test
  fun `cluster size greater than 1 - removes person from cluster - attaches person to new cluster - deletes from person match`() {
    stubDeletePersonMatch()

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

      // assert prisoner two (one that IS being excluded) has changed clusters
      val personTwo = personRepository.findByPrisonNumber(prisonerNumberTwo)!!
      assertThat(personOne.personKey!!.personUUID).isNotEqualTo(personTwo.personKey!!.personUUID)
      assertThat(personTwo.personKey!!.personEntities.size).isEqualTo(1)
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))
    }
  }

  @Test
  fun `cluster size of 1 - only deletes from person match`() {
    stubDeletePersonMatch()

    val prisonerNumberOne = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))

    personExclusionService.exclude { personRepository.findByPrisonNumber(prisonerNumberOne) }

    awaitAssert {
      val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
      assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)
      assertThat(personOne.personKey!!.personEntities.size).isEqualTo(1)
      wiremock.verify(1, deleteRequestedFor(urlEqualTo("/person")))
    }
  }

  @Test
  fun `error writing to db - does not call person match to delete`() {
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
      wiremock.verify(0, deleteRequestedFor(urlEqualTo("/person")))
    }
  }
}
