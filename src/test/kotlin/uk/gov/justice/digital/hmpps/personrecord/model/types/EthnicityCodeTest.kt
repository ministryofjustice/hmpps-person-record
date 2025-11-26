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
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.A1
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.A2
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.A3
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.A4
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.A9
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.B1
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.B2
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.B9
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.ETH03
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.M1
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.M2
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.M3
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.M9
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.MERGE
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.NS
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.O1
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.O2
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.O9
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.UN
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W1
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W2
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W3
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W4
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W5
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W8
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.W9
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.Z1
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.stream.Stream
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName as OffenderName

class EthnicityCodeTest {

  @ParameterizedTest
  @MethodSource("probationEthnicityCodes")
  fun `should map all probation ethnicity codes to cpr ethnicity codes`(probationEthnicityCode: String?, cprEthnicityCode: EthnicityCode?) {
    val probationCase = ProbationCase(
      identifiers = Identifiers(randomCrn()),
      ethnicity = Value(probationEthnicityCode),
      name = OffenderName(firstName = randomName()),
    )
    val person = Person.from(probationCase)
    assertThat(person.ethnicityCode).isEqualTo(cprEthnicityCode)
  }

  @ParameterizedTest
  @MethodSource("prisonEthnicityCodes")
  fun `should map all prison ethnicity codes to cpr ethnicity codes`(prisonEthnicityCode: String?, cprEthnicityCode: EthnicityCode?) {
    val prisoner = Prisoner(
      prisonNumber = randomPrisonNumber(),
      ethnicity = prisonEthnicityCode,
      lastName = randomName(),
      firstName = randomName(),
      dateOfBirth = randomDate(),
    )
    val person = Person.from(prisoner)
    assertThat(person.ethnicityCode).isEqualTo(cprEthnicityCode)
  }

  @ParameterizedTest
  @MethodSource("commonPlatformEthnicityCodes")
  fun `should map all common platform ethnicity codes to cpr ethnicity codes`(defendantEthnicityCode: String?, cprEthnicityCode: EthnicityCode?) {
    val defendant = Defendant(
      id = randomDefendantId(),
      personDefendant = PersonDefendant(
        personDetails = PersonDetails(
          ethnicity = Ethnicity(selfDefinedEthnicityCode = defendantEthnicityCode),
          lastName = randomName(),
        ),
      ),
    )
    val person = Person.from(defendant)
    assertThat(person.ethnicityCode).isEqualTo(cprEthnicityCode)
  }

  companion object {

    @JvmStatic
    fun probationEthnicityCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("A1", A1),
      Arguments.of("A2", A2),
      Arguments.of("A3", A3),
      Arguments.of("A4", A4),
      Arguments.of("A9", A9),
      Arguments.of("B1", B1),
      Arguments.of("B2", B2),
      Arguments.of("B9", B9),
      Arguments.of("M1", M1),
      Arguments.of("M2", M2),
      Arguments.of("M3", M3),
      Arguments.of("M9", M9),
      Arguments.of("NS", NS),
      Arguments.of("O2", O2),
      Arguments.of("O9", O9),
      Arguments.of("W1", W1),
      Arguments.of("W2", W2),
      Arguments.of("W3", W3),
      Arguments.of("W4", W4),
      Arguments.of("W5", W5),
      Arguments.of("W9", W9),
      Arguments.of("ETH03", ETH03),
      Arguments.of("ETH04", UN),
      Arguments.of("ETH05", UN),
      Arguments.of("O1", O1),
      Arguments.of("Z1", Z1),
      Arguments.of("Invalid", UN),
      Arguments.of(null, null),
    )

    @JvmStatic
    fun prisonEthnicityCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Asian/Asian British: Indian", A1),
      Arguments.of("Asian/Asian British: Pakistani", A2),
      Arguments.of("Asian/Asian British: Bangladeshi", A3),
      Arguments.of("Asian/Asian British: Chinese", A4),
      Arguments.of("Asian/Asian British: Any other backgr'nd", A9),
      Arguments.of("Black/Black British: Caribbean", B1),
      Arguments.of("Black/Black British: African", B2),
      Arguments.of("Black/Black British: Any other backgr'nd", B9),
      Arguments.of("Mixed: White and Black Caribbean", M1),
      Arguments.of("Mixed: White and Black African", M2),
      Arguments.of("Mixed: White and Asian", M3),
      Arguments.of("Mixed: Any other background", M9),
      Arguments.of("Needs to be confirmed following merge", MERGE),
      Arguments.of("Prefer not to say", NS),
      Arguments.of("Other: Arab", O2),
      Arguments.of("Other: Any other background", O9),
      Arguments.of("White: Eng./Welsh/Scot./N.Irish/British", W1),
      Arguments.of("White: Irish", W2),
      Arguments.of("White: Gypsy or Irish Traveller", W3),
      Arguments.of("White: Roma", "W5", "White : Roma"),
      Arguments.of("White: Any other background", W9),
      Arguments.of("Chinese", O1),
      Arguments.of("White : Irish Traveller/Gypsy", W8),
      Arguments.of("Invalid", UN),
      Arguments.of(null, null),
    )

    @JvmStatic
    fun commonPlatformEthnicityCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("A1", A1),
      Arguments.of("A2", A2),
      Arguments.of("A3", A3),
      Arguments.of("A4", A4),
      Arguments.of("A9", A9),
      Arguments.of("B1", B1),
      Arguments.of("B2", B2),
      Arguments.of("B9", B9),
      Arguments.of("M1", M1),
      Arguments.of("M2", M2),
      Arguments.of("M3", M3),
      Arguments.of("M9", M9),
      Arguments.of("NS", NS),
      Arguments.of("O2", O2),
      Arguments.of("O9", O9),
      Arguments.of("W1", W1),
      Arguments.of("W2", W2),
      Arguments.of("W3", W3),
      Arguments.of("W9", W9),
      Arguments.of("O1", O1),
      Arguments.of("Invalid", UN),
      Arguments.of(null, null),
    )
  }
}
