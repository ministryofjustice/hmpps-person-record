package uk.gov.justice.digital.hmpps.personrecord.api.controller.migrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCommonPlatformSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomLibraSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationSexCode

class MigrateSexCodesIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAllInBatch()
  }

  @Test
  fun `should migrate a sex code on the person to the primary name in pseudonym but not NOMIS or DELIUS records`() {
    val commonPlatformSexCode = randomCommonPlatformSexCode()
    val commonPlatformRecord = createPersonWithNewKey(
      Person(
        firstName = randomName(),
        defendantId = randomDefendantId(),
        lastName = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = SourceSystemType.COMMON_PLATFORM,
      ),
    )
    commonPlatformRecord.sexCode = commonPlatformSexCode.value
    personRepository.save(commonPlatformRecord)

    val libraSexCode = randomLibraSexCode()
    val libraRecord = createPersonWithNewKey(
      Person(
        firstName = randomName(),
        cId = randomCId(),
        lastName = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = SourceSystemType.LIBRA,
      ),
    )
    libraRecord.sexCode = libraSexCode.value
    personRepository.save(libraRecord)

    val prisonSexCode = randomPrisonSexCode()
    val prisonRecord = createPersonWithNewKey(
      Person(
        firstName = randomName(),
        prisonNumber = randomPrisonNumber(),
        lastName = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = SourceSystemType.NOMIS,
      ),
    )
    prisonRecord.sexCode = prisonSexCode.value
    personRepository.save(prisonRecord)

    val probationSexCode = randomProbationSexCode()
    val probationRecord = createPersonWithNewKey(
      Person(
        firstName = randomName(),
        crn = randomCrn(),
        lastName = randomName(),
        dateOfBirth = randomDate(),
        sourceSystem = SourceSystemType.DELIUS,
      ),
    )
    probationRecord.sexCode = probationSexCode.value
    personRepository.save(probationRecord)

    assertThat(commonPlatformRecord.getPrimaryName().sexCode).isNull()
    assertThat(libraRecord.getPrimaryName().sexCode).isNull()
    assertThat(prisonRecord.getPrimaryName().sexCode).isNull()
    assertThat(probationRecord.getPrimaryName().sexCode).isNull()

    webTestClient.post()
      .uri(MIGRATE_SEX_CODE_URL)
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val updatedCommonPlatformRecord = personRepository.findByDefendantId(commonPlatformRecord.defendantId!!)
      assertThat(updatedCommonPlatformRecord?.getPrimaryName()?.sexCode).isEqualTo(commonPlatformSexCode.value)
    }
    awaitAssert {
      val updatedLibraRecord = personRepository.findByCId(libraRecord.cId!!)
      assertThat(updatedLibraRecord?.getPrimaryName()?.sexCode).isEqualTo(libraSexCode.value)
    }
    awaitAssert {
      val updatedPrisonRecord = personRepository.findByPrisonNumber(prisonRecord.prisonNumber!!)
      assertThat(updatedPrisonRecord?.getPrimaryName()?.sexCode).isNull()
    }
    awaitAssert {
      val updatedProbationRecord = personRepository.findByCrn(probationRecord.crn!!)
      assertThat(updatedProbationRecord?.getPrimaryName()?.sexCode).isNull()
    }
  }

  companion object {
    private const val MIGRATE_SEX_CODE_URL = "/migrate/sex-codes"
  }
}
