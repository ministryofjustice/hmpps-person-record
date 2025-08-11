package uk.gov.justice.digital.hmpps.personrecord.migrations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import java.util.stream.Stream

class MigrateNationalityCodeIntTest : WebTestBase() {

  @ParameterizedTest
  @MethodSource("probationCodes")
  fun `migrates probation nationality to nationality code`(probationCode: String, cprCode: NationalityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomProbationPersonDetails())
    beforeMigrationPerson.nationality = probationCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.nationality).isEqualTo(probationCode)
    assertThat(beforeMigrationPerson.nationalities.size).isEqualTo(0)

    webTestClient.post()
      .uri("/migrate/nationality-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByCrn(beforeMigrationPerson.crn!!) }
      assertThat(afterMigrationPerson.nationalities.size).isEqualTo(1)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
    }
  }

  @ParameterizedTest
  @MethodSource("prisonCodes")
  fun `migrates prison nationality to nationality code`(prisonCode: String, cprCode: NationalityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomPrisonPersonDetails())
    beforeMigrationPerson.nationality = prisonCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.nationality).isEqualTo(prisonCode)
    assertThat(beforeMigrationPerson.nationalities.size).isEqualTo(0)

    webTestClient.post()
      .uri("/migrate/nationality-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByPrisonNumber(beforeMigrationPerson.prisonNumber!!) }
      assertThat(afterMigrationPerson.nationalities.size).isEqualTo(1)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
    }
  }

  @ParameterizedTest
  @MethodSource("libraCodes")
  fun `migrates libra nationality to nationality code`(libraCode: String, cprCode: NationalityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomLibraPersonDetails())
    beforeMigrationPerson.nationality = libraCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.nationality).isEqualTo(libraCode)
    assertThat(beforeMigrationPerson.nationalities.size).isEqualTo(0)

    webTestClient.post()
      .uri("/migrate/nationality-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByCId(beforeMigrationPerson.cId!!) }
      assertThat(afterMigrationPerson.nationalities.size).isEqualTo(1)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
    }
  }

  @ParameterizedTest
  @MethodSource("commonPlatformCodes")
  fun `migrates common platform nationality to nationality code`(commonPlatformCode: String, cprCode: NationalityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomCommonPlatformPersonDetails())
    beforeMigrationPerson.nationality = commonPlatformCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.nationality).isEqualTo(commonPlatformCode)
    assertThat(beforeMigrationPerson.nationalities.size).isEqualTo(0)

    webTestClient.post()
      .uri("/migrate/nationality-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByDefendantId(beforeMigrationPerson.defendantId!!) }
      assertThat(afterMigrationPerson.nationalities.size).isEqualTo(1)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.nationalities.first().nationalityCode?.description).isEqualTo(cprDescription)
    }
  }

  companion object {

    @JvmStatic
    fun probationCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("ALBA", NationalityCode.ALBA, "Albanian"),
      Arguments.of("ALGE", NationalityCode.ALGE, "Algerian"),
      Arguments.of("ASM", NationalityCode.ASM, "American Samoan"),
      Arguments.of("ANDO", NationalityCode.ANDO, "Andorran"),
      Arguments.of("ANGOL", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("AG", NationalityCode.AG, "Anguillan"),
      Arguments.of("ANTIG", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("ARGEN", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )

    @JvmStatic
    fun prisonCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Albanian", NationalityCode.ALBA, "Albanian"),
      Arguments.of("Algerian", NationalityCode.ALGE, "Algerian"),
      Arguments.of("American Samoan", NationalityCode.ASM, "American Samoan"),
      Arguments.of("Andorran", NationalityCode.ANDO, "Andorran"),
      Arguments.of("Angolan", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("Anguillan", NationalityCode.AG, "Anguillan"),
      Arguments.of("Citizen of Antigua and Barbuda", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("Argentine", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )

    @JvmStatic
    fun commonPlatformCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("AL", NationalityCode.ALBA, "Albanian"),
      Arguments.of("DZ", NationalityCode.ALGE, "Algerian"),
      Arguments.of("AD", NationalityCode.ANDO, "Andorran"),
      Arguments.of("AO", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("AG", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("AR", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )

    @JvmStatic
    fun libraCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Albanian", NationalityCode.ALBA, "Albanian"),
      Arguments.of("Algerian", NationalityCode.ALGE, "Algerian"),
      Arguments.of("Andorran", NationalityCode.ANDO, "Andorran"),
      Arguments.of("Angolan", NationalityCode.ANGOL, "Angolan"),
      Arguments.of("Citizen of Antigua and Barbuda", NationalityCode.ANTIG, "Citizen of Antigua and Barbuda"),
      Arguments.of("Argentinian", NationalityCode.ARGEN, "Argentine"),
      Arguments.of("UNKNOWN", NationalityCode.UNKNOWN, "Unknown"),
      Arguments.of("INVALID NATIONALITY CODE", NationalityCode.UNKNOWN, "Unknown"),
    )
  }
}
