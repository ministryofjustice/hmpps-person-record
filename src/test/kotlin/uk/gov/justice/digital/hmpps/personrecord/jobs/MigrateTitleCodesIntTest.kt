package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class MigrateTitleCodesIntTest : WebTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      reviewRepository.deleteAll()
      personKeyRepository.deleteAll()
    }

    @Test
    fun `should populate titleCode`() {
      val firstPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
      val firstTitleCode = firstPerson.pseudonyms.first().titleCode

      firstPerson.pseudonyms.first().titleCode = null

      personRepository.save(firstPerson)

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.pseudonyms?.first()?.titleCode).isEqualTo(null)
      }

      webTestClient.post()
        .uri("/admin/migrate-title-codes")
        .exchange()
        .expectStatus()
        .isOk

      awaitAssert {
        assertThat(personRepository.findByCrn(firstPerson.crn!!)?.pseudonyms?.first()?.titleCode).isEqualTo(firstTitleCode)
      }
    }
  }
}
