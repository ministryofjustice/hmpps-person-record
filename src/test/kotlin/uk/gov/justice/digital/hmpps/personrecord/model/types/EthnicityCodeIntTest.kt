package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Ethnicity
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.stream.Stream
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName as OffenderName

class EthnicityCodeIntTest : IntegrationTestBase() {

  @ParameterizedTest
  @MethodSource("probationEthnicityCodes")
  fun `should map all probation ethnicity codes to cpr ethnicity codes`(probationEthnicityCode: String?, cprEthnicityCode: EthnicityCode?, cprEthnicityCodeDescription: String?) {
    val probationCase = ProbationCase(
      identifiers = Identifiers(randomCrn()),
      ethnicity = Value(probationEthnicityCode),
      name = OffenderName(firstName = randomName()),
    )
    val person = createPerson(Person.from(probationCase))
    assertThat(person.ethnicityCode?.code).isEqualTo(cprEthnicityCode?.name)
    assertThat(person.ethnicityCode?.description).isEqualTo(cprEthnicityCodeDescription)
  }

  @ParameterizedTest
  @MethodSource("prisonEthnicityCodes")
  fun `should map all prison ethnicity codes to ethnicity codes`(prisonEthnicityCode: String?, cprEthnicityCode: EthnicityCode?, cprEthnicityCodeDescription: String?) {
    val prisoner = Prisoner(
      prisonNumber = randomPrisonNumber(),
      ethnicity = prisonEthnicityCode,
      lastName = randomName(),
      firstName = randomName(),
      dateOfBirth = randomDate(),
    )
    val person = createPerson(Person.from(prisoner))
    assertThat(person.ethnicityCode?.code).isEqualTo(cprEthnicityCode?.name)
    assertThat(person.ethnicityCode?.description).isEqualTo(cprEthnicityCodeDescription)
  }

  @ParameterizedTest
  @MethodSource("commonPlatformEthnicityCodes")
  fun `should map all common platform ethnicity codes to cpr ethnicity codes`(defendantEthnicityCode: String?, cprEthnicityCode: EthnicityCode?, cprEthnicityCodeDescription: String?) {
    val defendant = Defendant(
      id = randomDefendantId(),
      personDefendant = PersonDefendant(
        personDetails = PersonDetails(
          ethnicity = Ethnicity(selfDefinedEthnicityCode = defendantEthnicityCode),
          lastName = randomName(),
        ),
      ),
    )
    val person = createPerson(Person.from(defendant))
    assertThat(person.ethnicityCode?.code).isEqualTo(cprEthnicityCode?.name)
    assertThat(person.ethnicityCode?.description).isEqualTo(cprEthnicityCodeDescription)
  }

  companion object {

    @JvmStatic
    fun probationEthnicityCodes(): Stream<Arguments> = Stream.of(
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
      Arguments.of(null, null, null),
    )

    @JvmStatic
    fun prisonEthnicityCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Asian/Asian British: Indian", "A1", "Asian/Asian British : Indian"),
      Arguments.of("Asian/Asian British: Pakistani", "A2", "Asian/Asian British : Pakistani"),
      Arguments.of("Asian/Asian British: Bangladeshi", "A3", "Asian/Asian British : Bangladeshi"),
      Arguments.of("Asian/Asian British: Chinese", "A4", "Asian/Asian British : Chinese"),
      Arguments.of("Asian/Asian British: Any other backgr'nd", "A9", "Asian/Asian British : Any other backgr'nd"),
      Arguments.of("Black/Black British: Caribbean", "B1", "Black/Black British : Caribbean"),
      Arguments.of("Black/Black British: African", "B2", "Black/Black British : African"),
      Arguments.of("Black/Black British: Any other backgr'nd", "B9", "Black/Black British : Any other backgr'nd"),
      Arguments.of("Mixed: White and Black Caribbean", "M1", "Mixed : White and Black Caribbean"),
      Arguments.of("Mixed: White and Black African", "M2", "Mixed : White and Black African"),
      Arguments.of("Mixed: White and Asian", "M3", "Mixed : White and Asian"),
      Arguments.of("Mixed: Any other background", "M9", "Mixed : Any other background"),
      Arguments.of("Needs to be confirmed following merge", "MERGE", "Needs to be confirmed following merge"),
      Arguments.of("Prefer not to say", "NS", "Prefer not to say"),
      Arguments.of("Other: Arab", "O2", "Other : Arab"),
      Arguments.of("Other: Any other background", "O9", "Other : Any other background"),
      Arguments.of("White: Eng./Welsh/Scot./N.Irish/British", "W1", "White : Eng/Welsh/Scot/N.Irish/British"),
      Arguments.of("White: Irish", "W2", "White : Irish"),
      Arguments.of("White: Gypsy or Irish Traveller", "W3", "White : Gypsy or Irish Traveller"),
      Arguments.of("White: Roma", "W5", "White : Roma"),
      Arguments.of("White: Any other background", "W9", "White : Any other background"),
      Arguments.of("Chinese", "O1", "Chinese"),
      Arguments.of("White : Irish Traveller/Gypsy", "W8", "White : Irish Traveller/Gypsy"),
      Arguments.of("Invalid", "UN", "Unknown"),
      Arguments.of(null, null, null),
    )

    @JvmStatic
    fun commonPlatformEthnicityCodes(): Stream<Arguments> = Stream.of(
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
      Arguments.of("O1", "O1", "Chinese"),
      Arguments.of("Invalid", "UN", "Unknown"),
      Arguments.of(null, null, null),
    )
  }
}
