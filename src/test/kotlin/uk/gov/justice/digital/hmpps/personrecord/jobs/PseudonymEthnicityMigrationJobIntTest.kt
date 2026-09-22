package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity

@ExtendWith(OutputCaptureExtension::class)
class PseudonymEthnicityMigrationJobIntTest(
  @Autowired private val personRepo: PersonRepository,
  @Autowired private val transactionalEthnicityUpdater: TransactionalEthnicityUpdater,
) : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    deleteAllPersonData()
  }

  @Test
  fun `should migrate all non-null ethnicity codes to pseudonym`(output: CapturedOutput) {
    val pseudonymEthnicityMigrationJob = PseudonymEthnicityMigrationJob(personRepo, transactionalEthnicityUpdater, 0, 1)

    val personOneEthnicity = EthnicityCode.fromProbation(randomProbationEthnicity())
    val personOne = createPersonWithEthnicity(createRandomProbationPersonDetails(), personOneEthnicity)

    val personNullEthnicity = createPersonWithEthnicity(createRandomProbationPersonDetails(), null)

    val personTwoEthnicity = EthnicityCode.fromPrison(randomPrisonEthnicity())
    val personTwo = createPersonWithEthnicity(createRandomPrisonPersonDetails(), personTwoEthnicity)

    val personThreeEthnicity = EthnicityCode.fromCommonPlatform(randomCommonPlatformEthnicity())
    val personThree = createPersonWithEthnicity(createRandomCommonPlatformPersonDetails(), personThreeEthnicity)

    pseudonymEthnicityMigrationJob.run()

    val updatedPersonOne = personRepository.findByCrn(personOne.crn!!)!!
    assertThat(updatedPersonOne.getPrimaryName().ethnicityCode).isEqualTo(personOneEthnicity)
    updatedPersonOne.getAliases().forEach { assertThat(it.ethnicityCode).isNull() }

    val updatedPersonNullEthnicity = personRepository.findByCrn(personNullEthnicity.crn!!)!!
    assertThat(updatedPersonNullEthnicity.getPrimaryName().ethnicityCode).isNull()
    updatedPersonNullEthnicity.getAliases().forEach { assertThat(it.ethnicityCode).isNull() }

    val updatedPersonTwo = personRepository.findByPrisonNumber(personTwo.prisonNumber!!)!!
    assertThat(updatedPersonTwo.getPrimaryName().ethnicityCode).isEqualTo(personTwoEthnicity)
    updatedPersonTwo.getAliases().forEach { assertThat(it.ethnicityCode).isNull() }

    val updatedPersonThree = personRepository.findByDefendantId(personThree.defendantId!!)!!
    assertThat(updatedPersonThree.getPrimaryName().ethnicityCode).isEqualTo(personThreeEthnicity)
    updatedPersonThree.getAliases().forEach { assertThat(it.ethnicityCode).isNull() }

    assertThat(output.out).contains("PSEUDONYM_ETHNICITY_MIGRATION total elements: 3")
  }

  @Test
  fun `should restart from defined start page`(output: CapturedOutput) {
    val pseudonymEthnicityMigrationJob = PseudonymEthnicityMigrationJob(personRepo, transactionalEthnicityUpdater, 2, 1)

    val personOneEthnicity = EthnicityCode.fromProbation(randomProbationEthnicity())
    val personOne = createPersonWithEthnicity(createRandomProbationPersonDetails(), personOneEthnicity)

    val personTwoEthnicity = EthnicityCode.fromPrison(randomPrisonEthnicity())
    val personTwo = createPersonWithEthnicity(createRandomPrisonPersonDetails(), personTwoEthnicity)

    val personThreeEthnicity = EthnicityCode.fromCommonPlatform(randomCommonPlatformEthnicity())
    val personThree = createPersonWithEthnicity(createRandomCommonPlatformPersonDetails(), personThreeEthnicity)

    pseudonymEthnicityMigrationJob.run()

    val updatedPersonOne = personRepository.findByCrn(personOne.crn!!)!!
    assertThat(updatedPersonOne.getPrimaryName().ethnicityCode).isNull()

    val updatedPersonTwo = personRepository.findByPrisonNumber(personTwo.prisonNumber!!)!!
    assertThat(updatedPersonTwo.getPrimaryName().ethnicityCode).isNull()

    val updatedPersonThree = personRepository.findByDefendantId(personThree.defendantId!!)!!
    assertThat(updatedPersonThree.getPrimaryName().ethnicityCode).isEqualTo(personThreeEthnicity)

    assertThat(output.out).doesNotContain("PSEUDONYM_ETHNICITY_MIGRATION processing page 1/3")
    assertThat(output.out).doesNotContain("PSEUDONYM_ETHNICITY_MIGRATION processing page 2/3")
    assertThat(output.out).contains("PSEUDONYM_ETHNICITY_MIGRATION processing page 3/3")
  }

  fun createPersonWithEthnicity(person: Person, ethnicityCode: EthnicityCode?): PersonEntity {
    val personEntity = createPersonWithNewKey(person.copy(ethnicityCode = null))
    personEntity.ethnicityCode = ethnicityCode
    personRepository.save(personEntity)
    return personEntity
  }
}
