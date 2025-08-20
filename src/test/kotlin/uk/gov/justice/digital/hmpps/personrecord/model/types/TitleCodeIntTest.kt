package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDefendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.PersonDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.stream.Stream
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name as LibraName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName as OffenderName

class TitleCodeIntTest : IntegrationTestBase() {

  @ParameterizedTest
  @MethodSource("probationTitleCodes")
  fun `should map all probation title codes to cpr title codes`(probationTitleCode: String?, cprTitleCode: String?, cprTitleCodeDescription: String?) {
    val probationCase = ProbationCase(identifiers = Identifiers(randomCrn()), title = Value(probationTitleCode), name = OffenderName(firstName = randomName()))
    val person = createPerson(Person.from(probationCase))
    assertThat(person.getPrimaryName().titleCode?.code).isEqualTo(cprTitleCode)
    assertThat(person.getPrimaryName().titleCode?.description).isEqualTo(cprTitleCodeDescription)
  }

  @ParameterizedTest
  @MethodSource("prisonTitleCodes")
  fun `should map all prison title codes to cpr title codes`(prisonTitleCode: String?, cprTitleCode: String?, cprTitleCodeDescription: String?) {
    val prisoner = Prisoner(prisonNumber = randomPrisonNumber(), title = prisonTitleCode, lastName = randomName(), firstName = randomName(), dateOfBirth = randomDate())
    val person = createPerson(Person.from(prisoner))
    assertThat(person.getPrimaryName().titleCode?.code).isEqualTo(cprTitleCode)
    assertThat(person.getPrimaryName().titleCode?.description).isEqualTo(cprTitleCodeDescription)
  }

  @ParameterizedTest
  @MethodSource("commonPlatformTitleCodes")
  fun `should map all common platform title codes to cpr title codes`(defendantTitleCode: String?, cprTitleCode: String?, cprTitleCodeDescription: String?) {
    val defendant = Defendant(id = randomDefendantId(), personDefendant = PersonDefendant(personDetails = PersonDetails(title = defendantTitleCode, lastName = randomName())))
    val person = createPerson(Person.from(defendant))
    assertThat(person.getPrimaryName().titleCode?.code).isEqualTo(cprTitleCode)
    assertThat(person.getPrimaryName().titleCode?.description).isEqualTo(cprTitleCodeDescription)
  }

  @ParameterizedTest
  @MethodSource("libraTitleCodes")
  fun `should map all libra title codes to cpr title codes`(libraTitleCode: String?, cprTitleCode: String?, cprTitleCodeDescription: String?) {
    val libraHearingEvent = LibraHearingEvent(cId = randomCId(), name = LibraName(title = libraTitleCode))
    val person = createPerson(Person.from(libraHearingEvent))
    assertThat(person.getPrimaryName().titleCode?.code).isEqualTo(cprTitleCode)
    assertThat(person.getPrimaryName().titleCode?.description).isEqualTo(cprTitleCodeDescription)
  }

  companion object {

    @JvmStatic
    fun probationTitleCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("MR", "MR", "Mr"),
      Arguments.of("MRS", "MRS", "Mrs"),
      Arguments.of("MISS", "MISS", "Miss"),
      Arguments.of("MS", "MS", "Ms"),
      Arguments.of("MX", "MX", "Mx"),
      Arguments.of("REV", "REV", "Reverend"),
      Arguments.of("DME", "DME", "Dame"),
      Arguments.of("DR", "DR", "Dr"),
      Arguments.of("LDY", "LDY", "Lady"),
      Arguments.of("LRD", "LRD", "Lord"),
      Arguments.of("SIR", "SIR", "Sir"),
      Arguments.of("Invalid", "UN", "Unknown"),
      Arguments.of(null, null, null),
    )

    @JvmStatic
    fun prisonTitleCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Mr", "MR", "Mr"),
      Arguments.of("Mrs", "MRS", "Mrs"),
      Arguments.of("Miss", "MISS", "Miss"),
      Arguments.of("Ms", "MS", "Ms"),
      Arguments.of("Reverend", "REV", "Reverend"),
      Arguments.of("Father", "FR", "Father"),
      Arguments.of("Imam", "IMAM", "Imam"),
      Arguments.of("Rabbi", "RABBI", "Rabbi"),
      Arguments.of("Brother", "BR", "Brother"),
      Arguments.of("Sister", "SR", "Sister"),
      Arguments.of("Dame", "DME", "Dame"),
      Arguments.of("Dr", "DR", "Dr"),
      Arguments.of("Lady", "LDY", "Lady"),
      Arguments.of("Lord", "LRD", "Lord"),
      Arguments.of("Sir", "SIR", "Sir"),
      Arguments.of("Invalid", "UN", "Unknown"),
      Arguments.of(null, null, null),
    )

    @JvmStatic
    fun commonPlatformTitleCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Mr", "MR", "Mr"),
      Arguments.of("Mrs", "MRS", "Mrs"),
      Arguments.of("Miss", "MISS", "Miss"),
      Arguments.of("Ms", "MS", "Ms"),
      Arguments.of("Reverend", "REV", "Reverend"),
      Arguments.of("Father", "FR", "Father"),
      Arguments.of("Imam", "IMAM", "Imam"),
      Arguments.of("Rabbi", "RABBI", "Rabbi"),
      Arguments.of("Brother", "BR", "Brother"),
      Arguments.of("Sister", "SR", "Sister"),
      Arguments.of("Dame", "DME", "Dame"),
      Arguments.of("Dr", "DR", "Dr"),
      Arguments.of("Lady", "LDY", "Lady"),
      Arguments.of("Lord", "LRD", "Lord"),
      Arguments.of("Sir", "SIR", "Sir"),
      Arguments.of("Invalid", "UN", "Unknown"),
      Arguments.of(null, null, null),
    )

    @JvmStatic
    fun libraTitleCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("Mr", "MR", "Mr"),
      Arguments.of("MR", "MR", "Mr"),
      Arguments.of("Mrs", "MRS", "Mrs"),
      Arguments.of("MRS", "MRS", "Mrs"),
      Arguments.of("Miss", "MISS", "Miss"),
      Arguments.of("MISS", "MISS", "Miss"),
      Arguments.of("Ms", "MS", "Ms"),
      Arguments.of("MS", "MS", "Ms"),
      Arguments.of("Mx", "MX", "Mx"),
      Arguments.of("MX", "MX", "Mx"),
      Arguments.of("Reverend", "REV", "Reverend"),
      Arguments.of("Father", "FR", "Father"),
      Arguments.of("Imam", "IMAM", "Imam"),
      Arguments.of("Rabbi", "RABBI", "Rabbi"),
      Arguments.of("Brother", "BR", "Brother"),
      Arguments.of("Sister", "SR", "Sister"),
      Arguments.of("Dame", "DME", "Dame"),
      Arguments.of("Dr", "DR", "Dr"),
      Arguments.of("DR", "DR", "Dr"),
      Arguments.of("Lady", "LDY", "Lady"),
      Arguments.of("Lord", "LRD", "Lord"),
      Arguments.of("Sir", "SIR", "Sir"),
      Arguments.of("Invalid", "UN", "Unknown"),
      Arguments.of("", null, null),
      Arguments.of(null, null, null),
    )
  }
}
