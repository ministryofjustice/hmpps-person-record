package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class OverrideServiceIntTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    stubPersonMatchUpsert()
    stubPersonMatchScores()
  }

  @Test
  fun `should not overwrite include marker when excluding a record`() {
    val personACrn = randomCrn()
    val personBCrn = randomCrn()
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(personACrn))
      .addPerson(createRandomProbationPersonDetails(personBCrn))

    val personA = personRepository.findByCrn(personACrn)!!
    val personB = personRepository.findByCrn(personBCrn)!!
    val personD = createPersonWithNewKey(createRandomProbationPersonDetails())

    includeRecords(personA, personB)

    personA.assertIncluded(personB)

    excludeRecord(personB, personD)

    personB.assertExcluded(personD)
    personA.assertIncluded(personB)
  }
}
