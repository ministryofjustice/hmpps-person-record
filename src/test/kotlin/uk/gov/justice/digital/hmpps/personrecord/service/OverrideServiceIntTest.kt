package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase

class OverrideServiceIntTest : MessagingTestBase() {

  @BeforeEach
  fun beforeEach() {
    stubPersonMatchUpsert()
  }

  @Test
  fun `should not overwrite include marker when excluding a record`() {
    val personA = createPerson(createRandomProbationPersonDetails())
    val personB = createPerson(createRandomProbationPersonDetails())
    val personC = createPerson(createRandomProbationPersonDetails())
    createPersonKey()
      .addPerson(personA)
      .addPerson(personB)
      .addPerson(personC)

    val personD = createPerson(createRandomProbationPersonDetails())
    createPersonKey()
      .addPerson(personD)

    includeRecords(personA, personB, personC)

    personA.assertIncluded(personB)
    personA.assertIncluded(personC)

    excludeRecord(personC, personD)

    personC.assertExcluded(personD)
    personA.assertIncluded(personC)
  }
}
