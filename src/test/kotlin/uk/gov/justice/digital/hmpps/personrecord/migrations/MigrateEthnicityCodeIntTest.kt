package uk.gov.justice.digital.hmpps.personrecord.migrations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import java.util.stream.Stream

class MigrateEthnicityCodeIntTest : WebTestBase() {

  @ParameterizedTest
  @MethodSource("probationCodes")
  fun `migrates probation ethnicity to ethnicity code`(probationCode: String, cprCode: EthnicityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomProbationPersonDetails())
    beforeMigrationPerson.ethnicity = probationCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.ethnicity).isEqualTo(probationCode)

    webTestClient.post()
      .uri("/migrate/ethnicity-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByCrn(beforeMigrationPerson.crn!!) }
      assertThat(afterMigrationPerson.ethnicityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.ethnicityCode?.description).isEqualTo(cprDescription)
    }
  }

  @ParameterizedTest
  @MethodSource("prisonCodes")
  fun `migrates prison ethnicity to ethnicity code`(prisonCode: String, cprCode: EthnicityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomPrisonPersonDetails())
    beforeMigrationPerson.ethnicity = prisonCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.ethnicity).isEqualTo(prisonCode)

    webTestClient.post()
      .uri("/migrate/ethnicity-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByPrisonNumber(beforeMigrationPerson.prisonNumber!!) }
      assertThat(afterMigrationPerson.ethnicityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.ethnicityCode?.description).isEqualTo(cprDescription)
    }
  }

  @ParameterizedTest
  @MethodSource("abnormalEthnicityCodes")
  fun `migrates abnormal ethnicity to ethnicity code`(prisonCode: String) {
    val beforeMigrationPerson = createPerson(createRandomPrisonPersonDetails())
    beforeMigrationPerson.ethnicity = prisonCode
    personRepository.save(beforeMigrationPerson)
    webTestClient.post()
      .uri("/migrate/ethnicity-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson =
        awaitNotNullPerson { personRepository.findByPrisonNumber(beforeMigrationPerson.prisonNumber!!) }
      assertThat(afterMigrationPerson.ethnicityCode?.code).isNotNull()
      assertThat(afterMigrationPerson.ethnicityCode?.code).isNotEqualTo("UN")
      assertThat(afterMigrationPerson.ethnicityCode?.description).isNotNull()
      assertThat(afterMigrationPerson.ethnicityCode?.description).isNotEqualTo("UN")
    }
  }

  @ParameterizedTest
  @MethodSource("commonPlatformCodes")
  fun `migrates common platform ethnicity to ethnicity code`(commonPlatformCode: String, cprCode: EthnicityCode, cprDescription: String) {
    val beforeMigrationPerson = createPerson(createRandomCommonPlatformPersonDetails())
    beforeMigrationPerson.ethnicity = commonPlatformCode
    personRepository.save(beforeMigrationPerson)

    assertThat(beforeMigrationPerson.ethnicity).isEqualTo(commonPlatformCode)
    webTestClient.post()
      .uri("/migrate/ethnicity-codes")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByDefendantId(beforeMigrationPerson.defendantId!!) }
      assertThat(afterMigrationPerson.ethnicityCode?.code).isEqualTo(cprCode.name)
      assertThat(afterMigrationPerson.ethnicityCode?.description).isEqualTo(cprDescription)
    }
  }

  companion object {
    @JvmStatic
    fun abnormalEthnicityCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("A1"),
      Arguments.of("A2"),
      Arguments.of("A3"),
      Arguments.of("A4"),
      Arguments.of("A9"),
      Arguments.of("Asian/Asian British: Any other backgr'nd"),
      Arguments.of("Asian/Asian British: Bangladeshi"),
      Arguments.of("Asian/Asian British: Chinese"),
      Arguments.of("Asian/Asian British: Indian"),
      Arguments.of("Asian/Asian British: Pakistani"),
      Arguments.of("B1"),
      Arguments.of("B2"),
      Arguments.of("B9"),
      Arguments.of("Black/Black British: African"),
      Arguments.of("Black/Black British: Any other backgr'nd"),
      Arguments.of("Black/Black British: Caribbean"),
      Arguments.of("Chinese"),
      Arguments.of("ETH03"),
      Arguments.of("ETH04"),
      Arguments.of("ETH05"),
      Arguments.of("M1"),
      Arguments.of("M2"),
      Arguments.of("M3"),
      Arguments.of("M9"),
      Arguments.of("Mixed: Any other background"),
      Arguments.of("Mixed: White and Asian"),
      Arguments.of("Mixed: White and Black African"),
      Arguments.of("Mixed: White and Black Caribbean"),
      Arguments.of("Needs to be confirmed following merge"),
      Arguments.of("NS"),
      Arguments.of("O1"),
      Arguments.of("O2"),
      Arguments.of("O9"),
      Arguments.of("Other: Any other background"),
      Arguments.of("Other: Arab"),
      Arguments.of("Prefer not to say"),
      Arguments.of("W1"),
      Arguments.of("W2"),
      Arguments.of("W3"),
      Arguments.of("W4"),
      Arguments.of("W5"),
      Arguments.of("W9"),
      Arguments.of("White: Any other background"),
      Arguments.of("White: Eng./Welsh/Scot./N.Irish/British"),
      Arguments.of("White: Gypsy or Irish Traveller"),
      Arguments.of("White: Irish"),
      Arguments.of("White : Irish Traveller/Gypsy"),
      Arguments.of("White: Roma"),
      Arguments.of("Z1"),
    )

    @JvmStatic
    fun probationCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("A1", "A1", "Asian/Asian British : Indian"),
      Arguments.of("A2", "A2", "Asian/Asian British : Pakistani"),
      Arguments.of("A3", "A3", "Asian/Asian British : Bangladeshi"),
      Arguments.of("A4", "A4", "Asian/Asian British : Chinese"),
      Arguments.of("A9", "A9", "Asian/Asian British : Any other backgr'nd"),
      Arguments.of("B1", "B1", "Black/Black British : Caribbean"),
      Arguments.of("B2", "B2", "Black/Black British : African"),
      Arguments.of("B9", "B9", "Black/Black British : Any other backgr'nd"),
      Arguments.of("M1", "M1", "Mixed : White and Black Caribbean"),
      Arguments.of("M2", "M2", "Mixed : White and Black African"),
      Arguments.of("M3", "M3", "Mixed : White and Asian"),
      Arguments.of("M9", "M9", "Mixed : Any other background"),
      Arguments.of("NS", "NS", "Prefer not to say"),
      Arguments.of("O2", "O2", "Other : Arab"),
      Arguments.of("O9", "O9", "Other : Any other background"),
      Arguments.of("W1", "W1", "White : Eng/Welsh/Scot/N.Irish/British"),
      Arguments.of("W2", "W2", "White : Irish"),
      Arguments.of("W3", "W3", "White : Gypsy or Irish Traveller"),
      Arguments.of("W4", "W4", "White : Gypsy or Irish Traveller"),
      Arguments.of("W5", "W5", "White : Roma"),
      Arguments.of("W9", "W9", "White : Any other background"),
      Arguments.of("ETH03", "ETH03", "Other (historic)"),
      Arguments.of("ETH04", "ETH04", "Z_Dummy Ethnicity 04"),
      Arguments.of("ETH05", "ETH05", "Z_Dummy Ethnicity 05"),
      Arguments.of("O1", "O1", "Chinese"),
      Arguments.of("Z1", "Z1", "Missing (IAPS)"),
      Arguments.of("Invalid", "UN", "Unknown"),
    )

    @JvmStatic
    fun prisonCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Asian/Asian British: Indian", "A1", "Asian/Asian British : Indian"),
      Arguments.of("Asian/Asian British: Pakistani", "A2", "Asian/Asian British : Pakistani"),
      Arguments.of("Asian/Asian British: Bangladeshi", "A3", "Asian/Asian British : Bangladeshi"),
      Arguments.of("Asian/Asian British: Chinese", "A4", "Asian/Asian British : Chinese"),
      Arguments.of("Asian/Asian British: Any other backgr'nd", "A9", "Asian/Asian British : Any other backgr'nd"),
      Arguments.of("Black/Black British: Caribbean", "B1", "Black/Black British : Caribbean"),
      Arguments.of("Black/Black British: African", "B2", "Black/Black British : African"),
      Arguments.of("Black/Black British: Any other Backgr'nd", "B9", "Black/Black British : Any other backgr'nd"),
      Arguments.of("Mixed: White and Black Caribbean", "M1", "Mixed : White and Black Caribbean"),
      Arguments.of("Mixed: White and Black African", "M2", "Mixed : White and Black African"),
      Arguments.of("Mixed: White and Asian", "M3", "Mixed : White and Asian"),
      Arguments.of("Mixed: Any other background", "M9", "Mixed : Any other background"),
      Arguments.of("Needs to be confirmed following Merge", "MERGE", "Needs to be confirmed following merge"),
      Arguments.of("Prefer not to say", "NS", "Prefer not to say"),
      Arguments.of("Other: Arab", "O2", "Other : Arab"),
      Arguments.of("Other: Any other background", "O9", "Other : Any other background"),
      Arguments.of("White: Eng./Welsh/Scot./N.Irish/British", "W1", "White : Eng/Welsh/Scot/N.Irish/British"),
      Arguments.of("White: Irish", "W2", "White : Irish"),
      Arguments.of("White: Gypsy or Irish Traveller", "W3", "White : Gypsy or Irish Traveller"),
      Arguments.of("White: Roma", "W5", "White : Roma"),
      Arguments.of("White: Any other background", "W9", "White : Any other background"),
      Arguments.of("Chinese", "O1", "Chinese"),
      Arguments.of("White: Irish Traveller/Gypsy", "W8", "White : Irish Traveller/Gypsy"),
    )

    @JvmStatic
    fun commonPlatformCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("A1", "A1", "Asian/Asian British : Indian"),
      Arguments.of("A2", "A2", "Asian/Asian British : Pakistani"),
      Arguments.of("A3", "A3", "Asian/Asian British : Bangladeshi"),
      Arguments.of("A4", "A4", "Asian/Asian British : Chinese"),
      Arguments.of("A9", "A9", "Asian/Asian British : Any other backgr'nd"),
      Arguments.of("B1", "B1", "Black/Black British : Caribbean"),
      Arguments.of("B2", "B2", "Black/Black British : African"),
      Arguments.of("B9", "B9", "Black/Black British : Any other backgr'nd"),
      Arguments.of("M1", "M1", "Mixed : White and Black Caribbean"),
      Arguments.of("M2", "M2", "Mixed : White and Black African"),
      Arguments.of("M3", "M3", "Mixed : White and Asian"),
      Arguments.of("M9", "M9", "Mixed : Any other background"),
      Arguments.of("NS", "NS", "Prefer not to say"),
      Arguments.of("O2", "O2", "Other : Arab"),
      Arguments.of("O9", "O9", "Other : Any other background"),
      Arguments.of("W1", "W1", "White : Eng/Welsh/Scot/N.Irish/British"),
      Arguments.of("W2", "W2", "White : Irish"),
      Arguments.of("W3", "W3", "White : Gypsy or Irish Traveller"),
      Arguments.of("W9", "W9", "White : Any other background"),
      Arguments.of("ETH03", "ETH03", "Other (historic)"),
      Arguments.of("ETH04", "ETH04", "Z_Dummy Ethnicity 04"),
      Arguments.of("ETH05", "ETH05", "Z_Dummy Ethnicity 05"),
      Arguments.of("O1", "O1", "Chinese"),
      Arguments.of("Invalid", "UN", "Unknown"),
    )
  }
}
