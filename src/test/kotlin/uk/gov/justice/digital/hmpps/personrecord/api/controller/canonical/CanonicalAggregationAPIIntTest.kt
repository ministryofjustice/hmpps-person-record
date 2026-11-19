package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_ADMIN_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecordView
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class CanonicalAggregationAPIIntTest : WebTestBase() {

  @Nested
  inner class Aliases {
    @Test
    fun `should return latest modified from 2 records combining`() {
      val prisonDetails = createRandomPrisonPersonDetails()
      val probationDetails = createRandomProbationPersonDetails()

      val personKey = createPersonKey()
        .addPerson(prisonDetails)
        .addPerson(probationDetails)

      val prisonPerson = personRepository.findByPrisonNumber(prisonDetails.prisonNumber!!)!!
      val probationPerson = personRepository.findByCrn(probationDetails.crn!!)!!

      val responseBody = webTestClient.get()
        .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
        .authorised(listOf(PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<CanonicalRecordView>()
        .returnResult()
        .responseBody!!

      val canonicalAlias = CanonicalAlias.from(prisonPerson)!! + CanonicalAlias.from(probationPerson)!!

      assertThat(responseBody.canonicalRecord.firstName).isEqualTo(probationDetails.firstName)
      assertThat(responseBody.canonicalRecord.aliases).isEqualTo(canonicalAlias)
    }

    @Test
    fun `should return latest modified from 2 records deduplicating`() {
      val prisonDetails = createRandomPrisonPersonDetails()
      val probationDetails = createRandomProbationPersonDetails()

      val personKey = createPersonKey()
        .addPerson(prisonDetails)
        .addPerson(probationDetails.copy(aliases = prisonDetails.aliases))

      val latestPerson = personRepository.findByCrn(probationDetails.crn!!)!!
      val responseBody = webTestClient.get()
        .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
        .authorised(listOf(PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<CanonicalRecordView>()
        .returnResult()
        .responseBody!!

      val canonicalAlias = CanonicalAlias.from(latestPerson)!!

      assertThat(responseBody.canonicalRecord.firstName).isEqualTo(probationDetails.firstName)
      assertThat(responseBody.canonicalRecord.aliases).isEqualTo(canonicalAlias)
    }
  }

  @Nested
  inner class Addresses {
    @Test
    fun `should return latest modified from 2 records combining`() {
      val prisonDetails = createRandomPrisonPersonDetails()
      val probationDetails = createRandomProbationPersonDetails()

      val personKey = createPersonKey()
        .addPerson(prisonDetails)
        .addPerson(probationDetails)

      val responseBody = webTestClient.get()
        .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
        .authorised(listOf(PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<CanonicalRecordView>()
        .returnResult()
        .responseBody!!
      val prisonPerson = personRepository.findByPrisonNumber(prisonDetails.prisonNumber!!)!!
      val latestPerson = personRepository.findByCrn(probationDetails.crn!!)
      val canonicalAddress = prisonPerson.addresses.map { CanonicalAddress.from(it) } + latestPerson!!.addresses.map { CanonicalAddress.from(it) }

      assertThat(responseBody.canonicalRecord.firstName).isEqualTo(probationDetails.firstName)

      assertThat(responseBody.canonicalRecord.addresses)
        .usingRecursiveFieldByFieldElementComparator(
          RecursiveComparisonConfiguration.builder()
            .build(),
        )
        .containsExactlyInAnyOrderElementsOf(canonicalAddress)
    }

    @Test
    fun `should return latest modified from 2 records deduplicating`() {
      val prisonDetails = createRandomPrisonPersonDetails()
      val probationDetails = createRandomProbationPersonDetails()

      val personKey = createPersonKey()
        .addPerson(prisonDetails)
        .addPerson(probationDetails)

      val responseBody = webTestClient.get()
        .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
        .authorised(listOf(PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<CanonicalRecordView>()
        .returnResult()
        .responseBody!!

      val prisonPerson = personRepository.findByPrisonNumber(prisonDetails.prisonNumber!!)!!
      val latestPerson = personRepository.findByCrn(probationDetails.crn!!)!!
      val canonicalAddress = prisonPerson.addresses.map { CanonicalAddress.from(it) } + latestPerson.addresses.map { CanonicalAddress.from(it) }

      assertThat(responseBody.canonicalRecord.firstName).isEqualTo(probationDetails.firstName)
      assertThat(responseBody.canonicalRecord.addresses)
        .usingRecursiveFieldByFieldElementComparator(
          RecursiveComparisonConfiguration.builder()
            .build(),
        )
        .containsExactlyInAnyOrderElementsOf(canonicalAddress)
    }
  }

  @Nested
  inner class Sentences {
    @Test
    fun `should return latest modified from 2 records combining`() {
      val prisonDetails = createRandomPrisonPersonDetails()
      val latestPerson = createRandomProbationPersonDetails()

      val personKey = createPersonKey()
        .addPerson(prisonDetails)
        .addPerson(latestPerson)

      val responseBody = webTestClient.get()
        .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
        .authorised(listOf(PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<CanonicalRecordView>()
        .returnResult()
        .responseBody!!

      val canonicalSentences = prisonDetails.sentences.mapNotNull { it.sentenceDate } + latestPerson.sentences.mapNotNull { it.sentenceDate }

      assertThat(responseBody.canonicalRecord.firstName).isEqualTo(latestPerson.firstName)
      assertThat(responseBody.sentences.toList()).containsAll(canonicalSentences)
    }

    @Test
    fun `should return latest modified from 2 records deduplicating`() {
      val prisonPerson = createRandomPrisonPersonDetails()
      val probationDetails = createRandomProbationPersonDetails()

      val latestPerson = probationDetails.copy(sentences = probationDetails.sentences + prisonPerson.sentences)

      val personKey = createPersonKey()
        .addPerson(prisonPerson)
        .addPerson(latestPerson)

      val responseBody = webTestClient.get()
        .uri(canonicalAPIUrlAggregate(personKey.personUUID.toString()))
        .authorised(listOf(PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<CanonicalRecordView>()
        .returnResult()
        .responseBody!!

      val canonicalSentences = prisonPerson.sentences.map { it.sentenceDate } + latestPerson.sentences.map { it.sentenceDate }

      assertThat(responseBody.canonicalRecord.firstName).isEqualTo(probationDetails.firstName)
      assertThat(responseBody.sentences.toList()).containsAll(canonicalSentences)
    }
  }

  private fun canonicalAPIUrlAggregate(uuid: String) = "/canonical-record/$uuid"
}
