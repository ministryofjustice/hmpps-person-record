package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode

class MigrateNationalitiesIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should populate nationality code`() {
      val firstPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val secondPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val firstNationality = randomNationalityCode()
      val secondNationality = randomNationalityCode()
      setNationalityWithNoCode(firstPerson, firstNationality)
      setNationalityWithNoCode(secondPerson, secondNationality)

      assertThat(firstPerson.nationalities.first().nationalityCode).isNull()
      assertThat(secondPerson.nationalities.first().nationalityCode).isNull()

      webTestClient.post()
        .uri("/admin/migrate-nationalities")
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.nationalities?.first()?.nationalityCode).isEqualTo(firstNationality)
        assertThat(personRepository.findByCrn(secondPerson.crn!!)?.nationalities?.first()?.nationalityCode).isEqualTo(secondNationality)
      }
    }

    private fun setNationalityWithNoCode(
      firstPerson: PersonEntity,
      nationality: NationalityCode,
    ) {
      val nationalityCode = nationalityCodeRepository.findByCode(nationality.name)
      val nationalityEntityWithNoCode = NationalityEntity(nationalityCodeLegacy = nationalityCode)
      firstPerson.nationalities.add(nationalityEntityWithNoCode)
      nationalityEntityWithNoCode.person = firstPerson
      personRepository.save(firstPerson)
    }
  }
}
