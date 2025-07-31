package uk.gov.justice.digital.hmpps.personrecord.migrations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.util.stream.Stream

class MigrateTitleToTitleCodesIntTest : WebTestBase() {

  @ParameterizedTest
  @MethodSource("titleCodes")
  fun `migrates title to title codes`(title: String?, cprTitleCode: String?, cprTitleCodeDescription: String?) {
    val person = createPerson(createRandomPrisonPersonDetails())
    val pseudonymEntity = PseudonymEntity.aliasFrom(
      Alias(title = title, firstName = randomName(), lastName = randomName(), dateOfBirth = randomDate()),
      titleCode = null,
    )!!
    pseudonymEntity.person = person
    person.pseudonyms.add(pseudonymEntity)
    val beforeMigrationPerson = personRepository.save(person)

    assertThat(beforeMigrationPerson.getAliases().first().title).isEqualTo(title)
    assertThat(beforeMigrationPerson.getAliases().first().titleCode?.code).isNull()
    assertThat(beforeMigrationPerson.getAliases().first().titleCode?.description).isNull()

    webTestClient.post()
      .uri("/migrate/title-to-title-code")
      .exchange()
      .expectStatus()
      .isOk

    awaitAssert {
      val afterMigrationPerson = awaitNotNullPerson { personRepository.findByPrisonNumber(person.prisonNumber!!) }
      assertThat(afterMigrationPerson.getAliases().first().titleCode?.code).isEqualTo(cprTitleCode)
      assertThat(afterMigrationPerson.getAliases().first().titleCode?.description).isEqualTo(cprTitleCodeDescription)
    }
  }

  companion object {

    @JvmStatic
    fun titleCodes(): Stream<Arguments> = Stream.of(
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
