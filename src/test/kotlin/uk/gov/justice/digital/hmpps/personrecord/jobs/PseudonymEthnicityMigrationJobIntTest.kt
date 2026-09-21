package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@ExtendWith(OutputCaptureExtension::class)
class PseudonymEthnicityMigrationJobIntTest(
  @Autowired personRepo: PersonRepository,
  @Autowired transactionalEthnicityUpdater: TransactionalEthnicityUpdater,
) : IntegrationTestBase() {

  val pseudonymEthnicityMigrationJob = PseudonymEthnicityMigrationJob(personRepo, transactionalEthnicityUpdater, 0, 1)

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
  }

  @Test
  fun `should migrate all non-null ethnicity codes to pseudonym`(output: CapturedOutput) {
    val personOne = createPersonWithNewKey(createRandomProbationPersonDetails())
    val personNullEthnicity = createPersonWithNewKey(createRandomProbationPersonDetails().copy(ethnicityCode = null))
    val personTwo = createPersonWithNewKey(createRandomPrisonPersonDetails())
    val personThree = createPersonWithNewKey(createRandomCommonPlatformPersonDetails())

    pseudonymEthnicityMigrationJob.run()

    assertThat(personOne.getPrimaryName().ethnicityCode).isEqualTo(personOne.ethnicityCode)
    assertThat(personNullEthnicity.getPrimaryName().ethnicityCode).isNull()
    assertThat(personTwo.getPrimaryName().ethnicityCode).isEqualTo(personTwo.ethnicityCode)
    assertThat(personThree.getPrimaryName().ethnicityCode).isEqualTo(personThree.ethnicityCode)
    assertThat(output.out).contains("total elements: 3")
  }
}
