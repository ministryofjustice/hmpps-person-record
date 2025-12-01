package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationEthnicity

class MigrateEthnicityCodesIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      reviewRepository.deleteAll()
      personKeyRepository.deleteAll()
    }

    @Test
    fun `should populate ethnicityCode`() {
      val firstPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      val firstEthnicityCode = randomProbationEthnicity()

      setEthnicityCode(firstPerson, firstEthnicityCode)

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.ethnicityCode).isEqualTo(null)
      }

      webTestClient.post()
        .uri("/admin/migrate-ethnicity-codes")
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.ethnicityCode).isEqualTo(EthnicityCode.fromProbation(firstEthnicityCode))
      }
    }

    @Test
    fun `should convert discontinued probation ethnicityCodes to unknown`() {
      val firstPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

      val firstEthnicityCode = "ETH04"

      setEthnicityCode(firstPerson, firstEthnicityCode)

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.ethnicityCode).isEqualTo(null)
      }

      webTestClient.post()
        .uri("/admin/migrate-ethnicity-codes")
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.ethnicityCode).isEqualTo(EthnicityCode.fromProbation(firstEthnicityCode))
      }
    }

    private fun setEthnicityCode(person: PersonEntity, ethnicityCode: String) {
      val ethnicityCodeEntity: EthnicityCodeEntity? = ethnicityCodeRepository.findByCode(ethnicityCode)
      person.ethnicityCodeLegacy = ethnicityCodeEntity
      personRepository.save(person)
    }
  }
}
