package uk.gov.justice.digital.hmpps.personrecord.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase

class ClustersApiIntTest: WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAll()
    personKeyRepository.deleteAll()
  }

  @Test
  fun `should return list of clusters with composition `() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails())

    val responseType = object : ParameterizedTypeReference<PagedResponse<AdminCluster>>() {}
    val response = webTestClient.get()
      .uri(ADMIN_CLUSTERS_URL)
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!

    assertThat(response.content.size).isEqualTo(1)
    assertThat(response.totalElements).isEqualTo(1)
    assertThat(response.totalPages).isEqualTo(1)
    assertThat(response.page).isEqualTo(0)
    assertThat(response.size).isEqualTo(20)
    assertThat(response.content[0].uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.content[0].recordComposition.delius).isEqualTo(1)
    assertThat(response.content[0].recordComposition.nomis).isEqualTo(0)
    assertThat(response.content[0].recordComposition.libra).isEqualTo(0)
    assertThat(response.content[0].recordComposition.commonPlatform).isEqualTo(0)
  }

  @Test
  fun `should return list of multiple clusters with composition`() {
    val cluster1 = createPersonKey()
      .addPerson(createPerson(createRandomProbationPersonDetails()))
      .addPerson(createPerson(createRandomPrisonPersonDetails()))
      .addPerson(createPerson(createRandomCommonPlatformPersonDetails()))
      .addPerson(createPerson(createRandomLibraPersonDetails()))

    val cluster2 = createPersonKey()
      .addPerson(createPerson(createRandomProbationPersonDetails()))
      .addPerson(createPerson(createRandomProbationPersonDetails()))

    val responseType = object : ParameterizedTypeReference<PagedResponse<AdminCluster>>() {}
    val response = webTestClient.get()
      .uri(ADMIN_CLUSTERS_URL)
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!

    assertThat(response.content.size).isEqualTo(2)
    assertThat(response.totalElements).isEqualTo(2)
    assertThat(response.totalPages).isEqualTo(1)
    assertThat(response.page).isEqualTo(0)
    assertThat(response.size).isEqualTo(20)

    assertThat(response.content[0].uuid).isEqualTo(cluster1.personUUID.toString())
    assertThat(response.content[0].recordComposition.delius).isEqualTo(1)
    assertThat(response.content[0].recordComposition.nomis).isEqualTo(1)
    assertThat(response.content[0].recordComposition.libra).isEqualTo(1)
    assertThat(response.content[0].recordComposition.commonPlatform).isEqualTo(1)


    assertThat(response.content[1].uuid).isEqualTo(cluster2.personUUID.toString())
    assertThat(response.content[1].recordComposition.delius).isEqualTo(2)
    assertThat(response.content[1].recordComposition.nomis).isEqualTo(0)
    assertThat(response.content[1].recordComposition.libra).isEqualTo(0)
    assertThat(response.content[1].recordComposition.commonPlatform).isEqualTo(0)
  }

  @Test
  fun `should return default size of cluster if exceeds max`() {
    repeat(51) {
      createPersonKey().addPerson(createPerson(createRandomProbationPersonDetails()))
    }

    val responseType = object : ParameterizedTypeReference<PagedResponse<AdminCluster>>() {}
    val response = webTestClient.get()
      .uri("$ADMIN_CLUSTERS_URL?pageSize=9999999")
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!

    assertThat(response.content.size).isEqualTo(20)
    assertThat(response.totalElements).isEqualTo(51)
    assertThat(response.totalPages).isEqualTo(3)
    assertThat(response.page).isEqualTo(0)
    assertThat(response.size).isEqualTo(20)
  }

  @Test
  fun `should return specified page`() {
    repeat(20) {
      createPersonKey().addPerson(createPerson(createRandomProbationPersonDetails()))
    }

    val nextPageCluster = createPersonKey().addPerson(createPerson(createRandomProbationPersonDetails()))

    val responseType = object : ParameterizedTypeReference<PagedResponse<AdminCluster>>() {}
    val response = webTestClient.get()
      .uri("$ADMIN_CLUSTERS_URL?page=1")
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!

    assertThat(response.content.size).isEqualTo(1)
    assertThat(response.totalElements).isEqualTo(21)
    assertThat(response.totalPages).isEqualTo(2)
    assertThat(response.page).isEqualTo(0)
    assertThat(response.size).isEqualTo(20)

    assertThat(response.content[0].uuid).isEqualTo(nextPageCluster.personUUID.toString())
  }

  companion object {
    private const val ADMIN_CLUSTERS_URL = "/admin/clusters"
  }
}

data class PagedResponse<T>(
  val content: List<T>,
  val page: Int,
  val size: Int,
  val totalElements: Long,
  val totalPages: Int
)