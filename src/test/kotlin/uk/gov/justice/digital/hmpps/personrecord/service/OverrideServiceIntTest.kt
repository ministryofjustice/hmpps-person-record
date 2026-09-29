package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class OverrideServiceIntTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    stubPersonMatchUpsert()
  }

  @Test
  fun `should not overwrite include marker when excluding a record`() {
    val personACrn = randomCrn()
    val personBCrn = randomCrn()
    val personCCrn = randomCrn()
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(personACrn))
      .addPerson(createRandomProbationPersonDetails(personBCrn))
      .addPerson(createRandomProbationPersonDetails(personCCrn))

    val personA = personRepository.findByCrn(personACrn)!!
    val personB = personRepository.findByCrn(personBCrn)!!
    val personC = personRepository.findByCrn(personCCrn)!!
    val personD = createPersonWithNewKey(createRandomProbationPersonDetails())

    includeRecords(personA, personB, personC)

    personA.assertIncluded(personB)
    personA.assertIncluded(personC)

    excludeRecord(personC, personD)

    personC.assertExcluded(personD)
    personA.assertIncluded(personC)
  }
}
