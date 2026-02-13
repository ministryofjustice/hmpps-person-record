package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class PersonExclusionServiceTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personExclusionService: PersonExclusionService

  @Test
  fun `cluster size greater than 1 - unlinks person in question`() {
    val prisonerNumberOne = randomPrisonNumber()
    val prisonerNumberTwo = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberTwo))

    personExclusionService.exclude(prisonerNumberTwo)

    // assert prisoner one (one that is not being excluded) has not changed
    val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
    assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)
    assertThat(personOne.personKey!!.personEntities.size).isEqualTo(1)

    // assert prisoner two (one that IS being excluded) has changed
    val personTwo = personRepository.findByPrisonNumber(prisonerNumberTwo)!!
    assertThat(personOne.personKey!!.personUUID).isNotEqualTo(personTwo.personKey!!.personUUID)
    assertThat(personTwo.personKey!!.personEntities.size).isEqualTo(1)
  }

  @Test
  fun `cluster size of 1 - does nothing`() {
    val prisonerNumberOne = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))

    personExclusionService.exclude(prisonerNumberOne)

    val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
    assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)
    assertThat(personOne.personKey!!.personEntities.size).isEqualTo(1)
  }
}