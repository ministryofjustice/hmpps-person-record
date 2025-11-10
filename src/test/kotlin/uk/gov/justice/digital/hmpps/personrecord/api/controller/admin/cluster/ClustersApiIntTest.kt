package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin.cluster

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE

class ClustersApiIntTest : WebTestBase() {

  @BeforeEach
  fun beforeEach() {
    reviewRepository.deleteAll()
    personRepository.deleteAll()
    personKeyRepository.deleteAll()
  }

  @Test
  fun `should return list of clusters with composition`() {
    val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = UUIDStatusType.NEEDS_ATTENTION)

    val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
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
    assertThat(response.pagination.count).isEqualTo(1)
    assertThat(response.pagination.totalPages).isEqualTo(1)
    assertThat(response.pagination.page).isEqualTo(1)
    assertThat(response.pagination.perPage).isEqualTo(20)

    assertThat(response.content[0].uuid).isEqualTo(person.personKey?.personUUID.toString())
    assertThat(response.content[0].recordComposition[0].count).isEqualTo(0)
    assertThat(response.content[0].recordComposition[0].sourceSystem).isEqualTo(COMMON_PLATFORM)
    assertThat(response.content[0].recordComposition[1].count).isEqualTo(1)
    assertThat(response.content[0].recordComposition[1].sourceSystem).isEqualTo(DELIUS)
    assertThat(response.content[0].recordComposition[2].count).isEqualTo(0)
    assertThat(response.content[0].recordComposition[2].sourceSystem).isEqualTo(LIBRA)
    assertThat(response.content[0].recordComposition[3].count).isEqualTo(0)
    assertThat(response.content[0].recordComposition[3].sourceSystem).isEqualTo(NOMIS)
  }

  @Test
  fun `should not return clusters that are not NEEDS ATTENTION`() {
    createPersonWithNewKey(createRandomProbationPersonDetails(), status = ACTIVE)
    createPersonWithNewKey(createRandomProbationPersonDetails(), status = MERGED)
    createPersonWithNewKey(createRandomProbationPersonDetails(), status = RECLUSTER_MERGE)

    val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
    val response = webTestClient.get()
      .uri(ADMIN_CLUSTERS_URL)
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!

    assertThat(response.content.size).isEqualTo(0)
    assertThat(response.pagination.count).isEqualTo(0)
    assertThat(response.pagination.totalPages).isEqualTo(0)
    assertThat(response.pagination.page).isEqualTo(1)
  }

  @Test
  fun `should return list of multiple clusters with composition`() {
    val cluster1 = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
      .addPerson(createRandomProbationPersonDetails())
      .addPerson(createRandomPrisonPersonDetails())
      .addPerson(createRandomCommonPlatformPersonDetails())
      .addPerson(createRandomLibraPersonDetails())

    val cluster2 = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
      .addPerson(createRandomProbationPersonDetails())
      .addPerson(createRandomProbationPersonDetails())

    val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
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
    assertThat(response.pagination.count).isEqualTo(2)
    assertThat(response.pagination.totalPages).isEqualTo(1)
    assertThat(response.pagination.page).isEqualTo(1)
    assertThat(response.pagination.perPage).isEqualTo(20)

    assertThat(response.content[0].uuid).isEqualTo(cluster1.personUUID.toString())
    assertThat(response.content[0].recordComposition[0].count).isEqualTo(1)
    assertThat(response.content[0].recordComposition[0].sourceSystem).isEqualTo(COMMON_PLATFORM)
    assertThat(response.content[0].recordComposition[1].count).isEqualTo(1)
    assertThat(response.content[0].recordComposition[1].sourceSystem).isEqualTo(DELIUS)
    assertThat(response.content[0].recordComposition[2].count).isEqualTo(1)
    assertThat(response.content[0].recordComposition[2].sourceSystem).isEqualTo(LIBRA)
    assertThat(response.content[0].recordComposition[3].count).isEqualTo(1)
    assertThat(response.content[0].recordComposition[3].sourceSystem).isEqualTo(NOMIS)

    assertThat(response.content[1].uuid).isEqualTo(cluster2.personUUID.toString())
    assertThat(response.content[1].recordComposition.first { it.sourceSystem == COMMON_PLATFORM }.count).isEqualTo(0)
    assertThat(response.content[1].recordComposition[1].count).isEqualTo(2)
    assertThat(response.content[1].recordComposition[1].sourceSystem).isEqualTo(DELIUS)
    assertThat(response.content[1].recordComposition[2].count).isEqualTo(0)
    assertThat(response.content[1].recordComposition[2].sourceSystem).isEqualTo(LIBRA)
    assertThat(response.content[1].recordComposition[3].count).isEqualTo(0)
    assertThat(response.content[1].recordComposition[3].sourceSystem).isEqualTo(NOMIS)
  }

  @Test
  fun `should return specified page`() {
    repeat(20) {
      createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION).addPerson(createRandomProbationPersonDetails())
    }

    val nextPageCluster = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION).addPerson(createRandomProbationPersonDetails())

    val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
    val response = webTestClient.get()
      .uri("$ADMIN_CLUSTERS_URL?page=2")
      .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(responseType)
      .returnResult()
      .responseBody!!

    assertThat(response.content.size).isEqualTo(1)
    assertThat(response.pagination.count).isEqualTo(1)
    assertThat(response.pagination.totalPages).isEqualTo(2)
    assertThat(response.pagination.page).isEqualTo(2)
    assertThat(response.pagination.perPage).isEqualTo(20)

    assertThat(response.content[0].uuid).isEqualTo(nextPageCluster.personUUID.toString())
  }

  @Test
  fun `should return Access Denied 403 when role is wrong`() {
    val expectedErrorMessage = "Forbidden: Access Denied"
    webTestClient.get()
      .uri(ADMIN_CLUSTERS_URL)
      .authorised(listOf("UNSUPPORTED-ROLE"))
      .exchange()
      .expectStatus()
      .isForbidden
      .expectBody()
      .jsonPath("userMessage")
      .isEqualTo(expectedErrorMessage)
  }

  @Test
  fun `should return UNAUTHORIZED 401 when role is not set`() {
    webTestClient.get()
      .uri(ADMIN_CLUSTERS_URL)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  companion object {
    private const val ADMIN_CLUSTERS_URL = "/admin/clusters"
  }
}
