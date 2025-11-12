package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode

class PopulateNationalityCodeIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @Test
    fun `should populate nationality code`() {
      val firstPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val secondPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      setNationalityWithNoCode(firstPerson)
      setNationalityWithNoCode(secondPerson)

      assertThat(firstPerson.nationalities.first().nationalityCode).isNull()
      assertThat(secondPerson.nationalities.first().nationalityCode).isNull()

//      webTestClient.post()
//        .uri("/jobs/migrate-nationalities")
//        .exchange()
//        .expectStatus()
//        .isOk

//      nationalitiesRepository.findAll()?.forEach { nationality ->
//        assertThat(nationality.nationalityCode(isEqualTo(nationality.natiionalityCodeLegacy.code))
//      }
    }

    private fun setNationalityWithNoCode(firstPerson: PersonEntity) {
      val nationalityCode = nationalityCodeRepository.findByCode(randomNationalityCode().name)
      val nationalityEntityWithNoCode = NationalityEntity(nationalityCodeLegacy = nationalityCode)
      firstPerson.nationalities.add(nationalityEntityWithNoCode)
      nationalityEntityWithNoCode.person = firstPerson
      personRepository.save(firstPerson)
    }
  }
}
