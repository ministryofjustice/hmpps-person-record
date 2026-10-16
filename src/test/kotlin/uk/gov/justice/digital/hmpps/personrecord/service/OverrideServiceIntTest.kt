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
    val includeRecordCrn = randomCrn()
    val includeExcludeRecordCrn = randomCrn()
    createPersonKey()
      .addPerson(createRandomProbationPersonDetails(includeRecordCrn))
      .addPerson(createRandomProbationPersonDetails(includeExcludeRecordCrn))

    val includeRecord = personRepository.findByCrn(includeRecordCrn)!!
    val includeExcludeRecord = personRepository.findByCrn(includeExcludeRecordCrn)!!
    val excludeRecord = createPersonWithNewKey(createRandomProbationPersonDetails())

    includeRecords(includeRecord, includeExcludeRecord)

    includeRecord.assertIncluded(includeExcludeRecord)

    excludeRecord(includeExcludeRecord, excludeRecord)

    includeExcludeRecord.assertExcluded(excludeRecord)
    includeRecord.assertIncluded(includeExcludeRecord)
  }
}
