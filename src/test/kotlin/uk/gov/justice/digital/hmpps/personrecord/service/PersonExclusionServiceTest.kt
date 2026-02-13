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
  fun test() {
    val prisonerNumberOne = randomPrisonNumber()
    val prisonerNumberTwo = randomPrisonNumber()
    val originalPersonKeyEntity = createPersonKey()
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberOne))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumberTwo))

    personExclusionService.exclude(prisonerNumberTwo)

    // assert prisoner one (one that is not being excluded) has not changed
    val personOne = personRepository.findByPrisonNumber(prisonerNumberOne)!!
    assertThat(personOne.personKey!!.personUUID).isEqualTo(originalPersonKeyEntity.personUUID)

    val two = personRepository.findByPrisonNumber(prisonerNumberTwo)!!
    assertThat(personOne.personKey!!.personUUID).isNotEqualTo(two.personKey!!.personUUID)
  }
}